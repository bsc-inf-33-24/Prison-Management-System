package com.pms.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pms.api.cell.entity.Cell;
import com.pms.api.cell.repository.CellRepository;
import com.pms.api.prison.entity.Block;
import com.pms.api.prison.entity.Prison;
import com.pms.api.prison.repository.BlockRepository;
import com.pms.api.prison.repository.PrisonRepository;
import com.pms.api.security.jwt.JwtService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InmateHomeOfOriginApiIntegrationTest {

    private static final String INMATES_PATH = "/api/v1/inmates";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PrisonRepository prisonRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private CellRepository cellRepository;

    private String token;
    private UUID cellId;
        private Prison prison;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        prison = prisonRepository.saveAndFlush(
                new Prison("HOME-" + suffix.substring(0, 12), "Home Origin Test Prison", null));
        Block block = blockRepository.saveAndFlush(new Block(prison, "HOME-" + suffix.substring(0, 8)));
        Cell cell = cellRepository.saveAndFlush(new Cell(block, 1, 20));
        cellId = cell.getPublicUuid();

        User prisonOfficer = new User("Home Origin Test Officer", "h" + suffix.substring(0, 14),
                "unused-password-hash", "Prison Officer", Set.of());
        prisonOfficer.assignPrison(prison);
        token = jwtService.generateToken(userRepository.saveAndFlush(prisonOfficer));
    }

    @Test
    void createsAndRetrievesHomeOfOriginWithoutLegacyContactFields() throws Exception {
        ObjectNode request = validRequest();
        request.put("homeOfOrigin", "Blantyre");

        MvcResult createResult = mockMvc.perform(post(INMATES_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.homeOfOrigin").value("Blantyre"))
                .andExpect(jsonPath("$.contactPhone").doesNotExist())
                .andExpect(jsonPath("$.contactEmail").doesNotExist())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String inmateId = created.path("id").asText();
        mockMvc.perform(get(INMATES_PATH + "/{inmateId}", inmateId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeOfOrigin").value("Blantyre"))
                .andExpect(jsonPath("$.contactPhone").doesNotExist())
                .andExpect(jsonPath("$.contactEmail").doesNotExist());
    }

    @Test
    void homeOfOriginIsOptionalAndLimitedTo255Characters() throws Exception {
        ObjectNode requestWithoutHomeOfOrigin = validRequest();
        mockMvc.perform(post(INMATES_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithoutHomeOfOrigin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.homeOfOrigin").value(org.hamcrest.Matchers.nullValue()));

        ObjectNode oversizedRequest = validRequest();
        oversizedRequest.put("homeOfOrigin", "x".repeat(256));
        mockMvc.perform(post(INMATES_PATH)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oversizedRequest)))
                .andExpect(status().isBadRequest());
    }

        @Test
        void inmateDetailAccessAllowsAssignedRolesAndKeepsPrisonBoundaries() throws Exception {
                ObjectNode request = validRequest();
                MvcResult createResult = mockMvc.perform(post(INMATES_PATH)
                                                .header("Authorization", "Bearer " + token)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();
                String inmateId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                                .path("id").asText();

                expectDetailStatus(token, inmateId, 200);
                expectDetailStatus(tokenFor("Records Officer", prison), inmateId, 200);
                expectDetailStatus(tokenFor("Super Admin", null), inmateId, 200);
                expectDetailStatus(tokenFor("Commissioner", null), inmateId, 200);
                expectDetailStatus(tokenFor("Visitor Officer", prison), inmateId, 403);

                Prison otherPrison = prisonRepository.saveAndFlush(new Prison(
                                "OTHER-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                                "Other Test Prison", null));
                expectDetailStatus(tokenFor("Prison Officer", otherPrison), inmateId, 403);
        }

        private void expectDetailStatus(String userToken, String inmateId, int expectedStatus) throws Exception {
                mockMvc.perform(get(INMATES_PATH + "/{inmateId}", inmateId)
                                                .header("Authorization", "Bearer " + userToken))
                                .andExpect(status().is(expectedStatus));
        }

        private String tokenFor(String role, Prison assignedPrison) {
                String username = "role-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
                User user = new User("Home Origin " + role, username, "unused-password-hash", role, Set.of());
                if (assignedPrison != null) {
                        user.assignPrison(assignedPrison);
                }
                return jwtService.generateToken(userRepository.saveAndFlush(user));
        }

    private ObjectNode validRequest() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("firstName", "John");
        request.put("lastName", "Banda");
        request.put("dateOfBirth", LocalDate.of(1990, 4, 15).toString());
        request.put("gender", "Other");
        request.put("admissionDate", LocalDate.of(2025, 1, 10).toString());
        request.put("inmateType", "SENTENCED");
        request.put("bio", "Test inmate");
        request.put("medicalHistory", "Not recorded");
        request.put("nextOfKinName", "Alex Banda");
        request.put("nextOfKinRelationship", "Sibling");
        request.put("nextOfKinPhone", "+265000000000");
        request.put("cellId", cellId.toString());
        return request;
    }
}