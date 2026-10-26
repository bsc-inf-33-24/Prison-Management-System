package com.pms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VisitorApiFunctionalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InmateRepository inmateRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private String visitorOfficerToken;
    private String recordsOfficerToken;

    @BeforeEach
    void setUp() {
        User visitorOfficer = userRepository.saveAndFlush(
                new User("Visitor Officer", "vo_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                        "unused-password-hash", "Visitor Officer", Set.of()));
        User recordsOfficer = userRepository.saveAndFlush(
                new User("Records Officer", "ro_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                        "unused-password-hash", "Records Officer", Set.of()));

        visitorOfficerToken = jwtService.generateToken(visitorOfficer);
        recordsOfficerToken = jwtService.generateToken(recordsOfficer);
    }

    @Test
    void visitCanBeRecordedWithEmbeddedVisitorDetailsAndVisibleInHistory() throws Exception {
        Long inmateId = inmateRepository.saveAndFlush(
                new Inmate("INV-1001", "Grace", "Chirwa", LocalDate.of(1990, 1, 15), "Female",
                        LocalDate.of(2024, 6, 1))).getId();

        ObjectNode visitRequest = objectMapper.createObjectNode();
        visitRequest.put("inmateId", inmateId);
        visitRequest.put("visitorFullName", "John Banda");
        visitRequest.put("visitorIdNumber", "NID-22222");
        visitRequest.put("visitorPhone", "+265998877665");
        visitRequest.put("visitorRelationshipToInmate", "Brother");
        visitRequest.put("visitDateTime", "2026-10-10T14:30:00+02:00");
        visitRequest.put("durationMinutes", 60);

        mockMvc.perform(post("/api/v1/visits")
                        .header("Authorization", bearer(visitorOfficerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(visitRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visitor.fullName").value("John Banda"))
                .andExpect(jsonPath("$.visitor.idNumber").value("NID-22222"))
                .andExpect(jsonPath("$.inmate.id").value(inmateId));

        mockMvc.perform(get("/api/v1/inmates/{inmateId}/visits", inmateId)
                        .header("Authorization", bearer(recordsOfficerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].visitor.fullName").value("John Banda"));

        String response = mockMvc.perform(get("/api/v1/visits")
                        .param("inmateId", String.valueOf(inmateId))
                        .header("Authorization", bearer(recordsOfficerToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        assertNotNull(root.path("content"));
        assertEquals(1, root.path("content").size());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
