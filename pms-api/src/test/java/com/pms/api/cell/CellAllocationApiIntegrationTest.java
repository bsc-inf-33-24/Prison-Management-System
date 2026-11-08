package com.pms.api.cell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pms.api.cell.entity.Cell;
import com.pms.api.cell.entity.CellAllocation;
import com.pms.api.cell.repository.CellAllocationRepository;
import com.pms.api.cell.repository.CellRepository;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.prison.entity.Block;
import com.pms.api.prison.entity.Prison;
import com.pms.api.prison.repository.BlockRepository;
import com.pms.api.prison.repository.PrisonRepository;
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
    private PrisonRepository prisonRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private String recordsOfficerToken;
    private String superAdminToken;
    private Prison recordsOfficerPrison;

    @BeforeEach
    void setUp() {
        String username = "c" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        recordsOfficerPrison = prisonRepository.saveAndFlush(
                new Prison("C" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                        "Cell Test Prison", null));
        User recordsOfficer = userRepository.saveAndFlush(
                new User("Cell Test", username, "unused-password-hash", "Records Officer", Set.of()));
        recordsOfficer.assignPrison(recordsOfficerPrison);
        recordsOfficer = userRepository.saveAndFlush(recordsOfficer);
        recordsOfficerToken = jwtService.generateToken(recordsOfficer);
        User superAdmin = userRepository.saveAndFlush(new User("Cell Admin",
                "ca" + UUID.randomUUID().toString().replace("-", "").substring(0, 14),
                "unused-password-hash", "Super Admin", Set.of()));
        superAdminToken = jwtService.generateToken(superAdmin);
    }

    @Test
    void moveInmateToNewCellAndItCannotExceedCapacity() throws Exception {
        Block block = blockRepository.saveAndFlush(new Block(recordsOfficerPrison, "Cell Test Block"));
        Inmate inmate = new Inmate(
                "Dana", "White", LocalDate.of(1992, 9, 1),
                "Female", LocalDate.of(2024, 4, 5), null, LocalDate.of(2024, 4, 5), LocalDate.of(2024, 4, 5));
        inmate.assignPrison(recordsOfficerPrison);
        inmateRepository.saveAndFlush(inmate);
        Cell oldCell = cellRepository.saveAndFlush(new Cell(block, 4, 2));
        Cell newCell = cellRepository.saveAndFlush(new Cell(block, 5, 2));
        cellAllocationRepository.saveAndFlush(new CellAllocation(inmate, oldCell, Instant.now()));

        ObjectNode moveRequest = objectMapper.createObjectNode();
        moveRequest.put("cellId", newCell.getPublicUuid().toString());

        mockMvc.perform(put("/api/v1/inmates/" + inmate.getPublicUuid() + "/cell-allocation")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inmateId").value(inmate.getPublicUuid().toString()))
                .andExpect(jsonPath("$.oldCellId").value(oldCell.getPublicUuid().toString()))
                .andExpect(jsonPath("$.newCellId").value(newCell.getPublicUuid().toString()));

        CellAllocation activeAllocation = cellAllocationRepository
                .findByInmate_IdAndReleasedAtIsNull(inmate.getId())
                .orElseThrow();
        assertThat(activeAllocation.getCell().getId()).isEqualTo(newCell.getId());
    }

    @Test
    void recordsOfficerCanReadCellsButCannotAllocateInmates() throws Exception {
        mockMvc.perform(get("/api/v1/cells").header("Authorization", recordsOfficerBearerToken()))
                .andExpect(status().isOk());

        ObjectNode moveRequest = objectMapper.createObjectNode();
        moveRequest.put("cellId", UUID.randomUUID().toString());
        mockMvc.perform(put("/api/v1/inmates/" + UUID.randomUUID() + "/cell-allocation")
                        .header("Authorization", recordsOfficerBearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isForbidden());
    }

    private String bearerToken() {
        return "Bearer " + superAdminToken;
    }

    private String recordsOfficerBearerToken() {
        return "Bearer " + recordsOfficerToken;
    }
}
