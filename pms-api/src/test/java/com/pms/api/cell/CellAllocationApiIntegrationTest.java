package com.pms.api.cell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pms.api.cell.entity.Cell;
import com.pms.api.cell.entity.CellAllocation;
import com.pms.api.cell.repository.CellAllocationRepository;
import com.pms.api.cell.repository.CellRepository;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.security.jwt.JwtService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(com.pms.api.TestcontainersConfiguration.class)
class CellAllocationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InmateRepository inmateRepository;

    @Autowired
    private CellRepository cellRepository;

    @Autowired
    private CellAllocationRepository cellAllocationRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private String recordsOfficerToken;

    @BeforeEach
    void setUp() {
        String username = "cell-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        User recordsOfficer = userRepository.saveAndFlush(
                new User("Cell Test", username, "unused-password-hash", "Records Officer", Set.of()));
        recordsOfficerToken = jwtService.generateToken(recordsOfficer);
    }

    @Test
    void allocateIntoMatchingCellAndListOccupancy() throws Exception {
        Inmate inmate = inmateRepository.saveAndFlush(new Inmate(
                uniqueInmateNumber(), "Alice", "Jones", LocalDate.of(1995, 1, 10),
                "Female", LocalDate.of(2024, 1, 1), "Minimum", "Main Facility"));
        Cell cell = cellRepository.saveAndFlush(new Cell("A", 1, 2, "Minimum"));

        ObjectNode request = objectMapper.createObjectNode();
        request.put("cellId", cell.getId());

        mockMvc.perform(post("/api/inmates/" + inmate.getId() + "/cell-allocation")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inmateId").value(inmate.getId()))
                .andExpect(jsonPath("$.cellId").value(cell.getId()))
                .andExpect(jsonPath("$.currentOccupancy").value(1))
                .andExpect(jsonPath("$.availableSpaces").value(1));

        mockMvc.perform(get("/api/cells")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].capacity").isNumber())
                .andExpect(jsonPath("$[0].currentOccupancy").isNumber());
    }

    @Test
    void rejectDuplicateAllocationAndWrongClassification() throws Exception {
        Inmate inmate = inmateRepository.saveAndFlush(new Inmate(
                uniqueInmateNumber(), "Bob", "Brown", LocalDate.of(1988, 5, 10),
                "Male", LocalDate.of(2023, 7, 1), "Medium", "Main Facility"));
        Cell matchingCell = cellRepository.saveAndFlush(new Cell("A", 2, 2, "Medium"));
        cellAllocationRepository.saveAndFlush(new CellAllocation(inmate, matchingCell, Instant.now()));

        ObjectNode request = objectMapper.createObjectNode();
        request.put("cellId", matchingCell.getId());

        mockMvc.perform(post("/api/inmates/" + inmate.getId() + "/cell-allocation")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        Inmate wrongClassificationInmate = inmateRepository.saveAndFlush(new Inmate(
                uniqueInmateNumber(), "Charlie", "Black", LocalDate.of(1990, 3, 8),
                "Male", LocalDate.of(2024, 6, 1), "Maximum", "Main Facility"));
        Cell wrongCell = cellRepository.saveAndFlush(new Cell("B", 3, 1, "Medium"));

        ObjectNode wrongRequest = objectMapper.createObjectNode();
        wrongRequest.put("cellId", wrongCell.getId());

        mockMvc.perform(post("/api/inmates/" + wrongClassificationInmate.getId() + "/cell-allocation")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void moveInmateToNewCellAndItCannotExceedCapacity() throws Exception {
        Inmate inmate = inmateRepository.saveAndFlush(new Inmate(
                uniqueInmateNumber(), "Dana", "White", LocalDate.of(1992, 9, 1),
                "Female", LocalDate.of(2024, 4, 5), "Minimum", "Main Facility"));
        Cell oldCell = cellRepository.saveAndFlush(new Cell("C", 4, 2, "Minimum"));
        Cell newCell = cellRepository.saveAndFlush(new Cell("C", 5, 2, "Minimum"));
        cellAllocationRepository.saveAndFlush(new CellAllocation(inmate, oldCell, Instant.now()));

        ObjectNode moveRequest = objectMapper.createObjectNode();
        moveRequest.put("cellId", newCell.getId());

        mockMvc.perform(put("/api/inmates/" + inmate.getId() + "/cell-allocation")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inmateId").value(inmate.getId()))
                .andExpect(jsonPath("$.oldCellId").value(oldCell.getId()))
                .andExpect(jsonPath("$.newCellId").value(newCell.getId()));

        assertThat(cellAllocationRepository.findByInmateId(inmate.getId())).isPresent();
        assertThat(cellAllocationRepository.findByInmateId(inmate.getId()).get().getCell().getId()).isEqualTo(newCell.getId());
    }

    private String bearerToken() {
        return recordsOfficerToken;
    }

    private String uniqueInmateNumber() {
        return "CELL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
