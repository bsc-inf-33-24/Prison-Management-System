package com.pms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.pms.api.prison.entity.Prison;
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
    private PrisonRepository prisonRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private String visitorOfficerToken;
    private String recordsOfficerToken;
    private Prison testPrison;

    @BeforeEach
    void setUp() {
        testPrison = prisonRepository.saveAndFlush(new Prison(
                "V" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                "Visit Test Prison", null));
        User visitorOfficer = new User(
                "Visitor Officer", "vo_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                "unused-password-hash", "Visitor Officer", Set.of());
        visitorOfficer.assignPrison(testPrison);
        userRepository.saveAndFlush(visitorOfficer);
        User recordsOfficer = new User(
                "Records Officer", "ro_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                "unused-password-hash", "Records Officer", Set.of());
        recordsOfficer.assignPrison(testPrison);
        userRepository.saveAndFlush(recordsOfficer);

        visitorOfficerToken = jwtService.generateToken(visitorOfficer);
        recordsOfficerToken = jwtService.generateToken(recordsOfficer);
    }

    @Test
    void visitRecordsStartEndAndCalculatedDurationAndAppearsInHistory() throws Exception {
        Inmate inmate = new Inmate("Grace", "Chirwa", LocalDate.of(1990, 1, 15), "Female",
                LocalDate.of(2024, 6, 1), null, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 1));
        inmate.assignPrison(testPrison);
        UUID inmateId = inmateRepository.saveAndFlush(inmate).getPublicUuid();

        ObjectNode visitRequest = objectMapper.createObjectNode();
        visitRequest.put("visitorFullName", "John Banda");
        visitRequest.put("inmateId", inmateId.toString());
        visitRequest.put("visitDate", "2026-10-10");
        visitRequest.put("startTime", "14:30");
        visitRequest.put("endTime", "15:30");

        mockMvc.perform(post("/api/v1/visits/family-friends")
                        .header("Authorization", bearer(visitorOfficerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(visitRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visitor.fullName").value("John Banda"))
                .andExpect(jsonPath("$.inmate.id").value(inmateId.toString()))
                .andExpect(jsonPath("$.visitDate").value("2026-10-10"))
                .andExpect(jsonPath("$.startDateTime").value("2026-10-10T14:30:00+02:00"))
                .andExpect(jsonPath("$.endDateTime").value("2026-10-10T15:30:00+02:00"))
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.legalCaseNumber").doesNotExist())
                .andExpect(jsonPath("$.legalRepresentative").doesNotExist());

        mockMvc.perform(get("/api/v1/inmates/{inmateId}/visits", inmateId.toString())
                        .header("Authorization", bearer(recordsOfficerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].visitor.fullName").value("John Banda"))
                .andExpect(jsonPath("$.content[0].visitDate").value("2026-10-10"))
                .andExpect(jsonPath("$.content[0].startDateTime").value("2026-10-10T12:30:00Z"))
                .andExpect(jsonPath("$.content[0].endDateTime").value("2026-10-10T13:30:00Z"))
                .andExpect(jsonPath("$.content[0].durationMinutes").value(60));

        String response = mockMvc.perform(get("/api/v1/visits")
                        .param("inmateNumber", "INV-1001")
                        .header("Authorization", bearer(recordsOfficerToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        assertNotNull(root.path("content"));
        assertEquals(1, root.path("content").size());
    }

    @Test
    void recordsOfficerCanRecordAndViewFamilyAndLegalVisitsInUnifiedModule() throws Exception {
        Inmate inmate = new Inmate("Martha", "Phiri", LocalDate.of(1988, 4, 12), "Female",
                LocalDate.of(2024, 6, 1), null, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 1));
        inmate.assignPrison(testPrison);
        UUID inmateId = inmateRepository.saveAndFlush(inmate).getPublicUuid();

        ObjectNode familyRequest = objectMapper.createObjectNode();
        familyRequest.put("visitorFullName", "Tadala Banda");
        familyRequest.put("inmateId", inmateId.toString());
        familyRequest.put("visitDate", "2026-10-10");
        familyRequest.put("startTime", "09:00");
        familyRequest.put("endTime", "09:30");
        mockMvc.perform(post("/api/v1/visits/family-friends")
                        .header("Authorization", bearer(recordsOfficerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(familyRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visitType").value("FAMILY"));

        ObjectNode legalRequest = objectMapper.createObjectNode();
        legalRequest.put("legalCaseNumber", "CASE-2026-42");
        legalRequest.put("legalRepresentative", "Grace Chirwa");
        legalRequest.put("inmateId", inmateId.toString());
        legalRequest.put("visitDate", "2026-10-10");
        legalRequest.put("startTime", "10:00");
        legalRequest.put("endTime", "10:45");
        mockMvc.perform(post("/api/v1/visits/legal")
                        .header("Authorization", bearer(recordsOfficerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(legalRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visitType").value("LEGAL"));

        String historyResponse = mockMvc.perform(get("/api/v1/inmates/{inmateId}/visits", inmateId.toString())
                        .header("Authorization", bearer(recordsOfficerToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode visits = objectMapper.readTree(historyResponse).path("content");
        assertEquals(2, visits.size());
        JsonNode familyVisit = null;
        JsonNode legalVisit = null;
        for (JsonNode visit : visits) {
            if ("FAMILY".equals(visit.path("visitType").asText())) {
                familyVisit = visit;
            } else if ("LEGAL".equals(visit.path("visitType").asText())) {
                legalVisit = visit;
            }
        }
        assertNotNull(familyVisit);
        assertEquals("Tadala Banda", familyVisit.path("visitorName").asText());
        assertNotNull(legalVisit);
        assertEquals("CASE-2026-42", legalVisit.path("legalCaseNumber").asText());
        assertEquals("Grace Chirwa", legalVisit.path("legalRepresentative").asText());

        String familyHistory = mockMvc.perform(get(
                        "/api/v1/inmates/{inmateId}/visits/family-friends", inmateId.toString())
                        .header("Authorization", bearer(recordsOfficerToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode familyVisits = objectMapper.readTree(familyHistory).path("content");
        assertEquals(1, familyVisits.size());
        assertEquals("FAMILY", familyVisits.get(0).path("visitType").asText());
        assertEquals("Tadala Banda", familyVisits.get(0).path("visitorName").asText());
        assertFalse(familyVisits.get(0).has("legalCaseNumber"));
        assertFalse(familyVisits.get(0).has("legalRepresentative"));

        String filteredSearchHistory = mockMvc.perform(get("/api/v1/visits")
                        .param("inmateId", inmateId.toString())
                        .param("visitType", "FAMILY")
                        .header("Authorization", bearer(recordsOfficerToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode filteredSearchVisits = objectMapper.readTree(filteredSearchHistory).path("content");
        assertEquals(1, filteredSearchVisits.size());
        assertEquals("FAMILY", filteredSearchVisits.get(0).path("visitType").asText());
        assertFalse(filteredSearchVisits.get(0).has("legalCaseNumber"));
        assertFalse(filteredSearchVisits.get(0).has("legalRepresentative"));

        String filteredInmateHistory = mockMvc.perform(get(
                        "/api/v1/inmates/{inmateId}/visits", inmateId.toString())
                        .param("visitType", "FAMILY")
                        .header("Authorization", bearer(recordsOfficerToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode filteredInmateVisits = objectMapper.readTree(filteredInmateHistory).path("content");
        assertEquals(1, filteredInmateVisits.size());
        assertEquals("FAMILY", filteredInmateVisits.get(0).path("visitType").asText());
        assertFalse(filteredInmateVisits.get(0).has("legalCaseNumber"));
        assertFalse(filteredInmateVisits.get(0).has("legalRepresentative"));

        String legalHistory = mockMvc.perform(get(
                        "/api/v1/inmates/{inmateId}/visits/legal", inmateId.toString())
                        .header("Authorization", bearer(recordsOfficerToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode legalVisits = objectMapper.readTree(legalHistory).path("content");
        assertEquals(1, legalVisits.size());
        assertEquals("LEGAL", legalVisits.get(0).path("visitType").asText());
        assertEquals("CASE-2026-42", legalVisits.get(0).path("legalCaseNumber").asText());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
