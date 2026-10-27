package com.pms.api.cell;

import static org.assertj.core.api.Assertions.assertThat;
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
    void moveInmateToNewCellAndItCannotExceedCapacity() throws Exception {
        Inmate inmate = inmateRepository.saveAndFlush(new Inmate(
                uniqueInmateNumber(), "Dana", "White", LocalDate.of(1992, 9, 1),
                "Female", LocalDate.of(2024, 4, 5), null, LocalDate.of(2024, 4, 5), LocalDate.of(2024, 4, 5)));
        Cell oldCell = cellRepository.saveAndFlush(new Cell("C", 4, 2));
        Cell newCell = cellRepository.saveAndFlush(new Cell("C", 5, 2));
        cellAllocationRepository.saveAndFlush(new CellAllocation(inmate, oldCell, Instant.now()));

        ObjectNode moveRequest = objectMapper.createObjectNode();
        moveRequest.put("cellId", newCell.getPublicUuid().toString());

        mockMvc.perform(put("/api/inmates/" + inmate.getId() + "/cell-allocation")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inmateId").value(inmate.getId()))
                .andExpect(jsonPath("$.oldCellId").value(oldCell.getPublicUuid().toString()))
                .andExpect(jsonPath("$.newCellId").value(newCell.getPublicUuid().toString()));

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
