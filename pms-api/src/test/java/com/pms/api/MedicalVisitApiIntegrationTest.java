package com.pms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.security.jwt.JwtService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.security.access.prepost.PreAuthorize;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MedicalVisitApiIntegrationTest {

    private static final String MEDICAL_VISITS_PATH = "/api/v1/medical-visits";

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

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void recordsEachVisitAndReturnsHistoryInDateOrder() throws Exception {
        Inmate inmate = createInmate();
        User medicalOfficer = createUser("Medical Officer");
        String token = jwtService.generateToken(medicalOfficer);

        MvcResult olderVisit = postVisit(token, medicalVisitRequest(
                inmate.getPublicUuid(), LocalDate.now().minusDays(1), "Headache", "Tension headache"));
        assertEquals(201, olderVisit.getResponse().getStatus());
        assertEquals("Tension headache",
                objectMapper.readTree(olderVisit.getResponse().getContentAsString()).path("diagnosis").asText());

        MvcResult newerVisit = postVisit(token, medicalVisitRequest(
                inmate.getPublicUuid(), LocalDate.now(), "Fever", "Viral infection"));
        assertEquals(201, newerVisit.getResponse().getStatus());

        MvcResult historyResult = mockMvc.perform(get(MEDICAL_VISITS_PATH)
                        .param("inmateNumber", inmate.getInmateNumber())
                        .param("name", "medical test")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        assertEquals(200, historyResult.getResponse().getStatus());

        JsonNode history = objectMapper.readTree(historyResult.getResponse().getContentAsString());
        assertEquals(inmate.getPublicUuid().toString(), history.path("inmateId").asText());
        assertEquals(inmate.getInmateNumber(), history.path("inmateNumber").asText());
        assertEquals(2, history.path("visits").size());
        assertEquals(objectMapper.readTree(newerVisit.getResponse().getContentAsString()).path("id").asText(),
                history.path("visits").get(0).path("id").asText());
        assertEquals("Fever", history.path("visits").get(0).path("sickness").asText());
        assertEquals("Viral infection", history.path("visits").get(0).path("diagnosis").asText());
        assertEquals("Paracetamol", history.path("visits").get(0).path("medicationsProvided").asText());
        assertEquals("Review in 48 hours.", history.path("visits").get(0).path("recommendations").asText());
        assertEquals(medicalOfficer.getFullName(), history.path("visits").get(0).path("recordedBy").asText());

        MvcResult nonMatchingHistory = mockMvc.perform(get(MEDICAL_VISITS_PATH)
                        .param("inmateNumber", inmate.getInmateNumber())
                        .param("name", "someone else")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        assertEquals(200, nonMatchingHistory.getResponse().getStatus());
        assertEquals(0, objectMapper.readTree(nonMatchingHistory.getResponse().getContentAsString())
                .path("visits").size());
    }

    @Test
    void medicalEndpointsRequireMedicalOfficerRoleAndAuthentication() throws Exception {
        Inmate inmate = createInmate();
        String body = medicalVisitRequest(inmate.getPublicUuid(), LocalDate.now(), "Cough", "Bronchitis");

        assertEquals(401, postVisit(null, body).getResponse().getStatus());
        assertEquals(401, mockMvc.perform(get(MEDICAL_VISITS_PATH)
                        .param("inmateNumber", inmate.getInmateNumber()))
                .andReturn().getResponse().getStatus());

        for (String role : User.ALLOWED_ROLES) {
            if ("Medical Officer".equals(role) || "Super Admin".equals(role)) {
                continue;
            }
            String token = jwtService.generateToken(createUser(role));
            assertEquals(403, postVisit(token, body).getResponse().getStatus(), role + " cannot record visits.");
            assertEquals(403, mockMvc.perform(get(MEDICAL_VISITS_PATH)
                            .param("inmateNumber", inmate.getInmateNumber())
                            .header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getStatus(), role + " cannot view medical history.");
        }
    }

    @Test
    void superAdminCanRecordAndSearchMedicalVisits() throws Exception {
        Inmate inmate = createInmate();
        String token = jwtService.generateToken(createUser("Super Admin"));

        assertEquals(201, postVisit(token, medicalVisitRequest(
                inmate.getPublicUuid(), LocalDate.now(), "Cough", "Bronchitis"))
                .getResponse().getStatus());

        MvcResult history = mockMvc.perform(get(MEDICAL_VISITS_PATH)
                        .param("inmateNumber", inmate.getInmateNumber())
                        .param("name", "MEDICAL")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        assertEquals(200, history.getResponse().getStatus());
        assertEquals(1, objectMapper.readTree(history.getResponse().getContentAsString())
                .path("visits").size());
    }

    @Test
    void medicalControllerMethodsDeclareRoleAuthorization() {
        var handlers = handlerMapping.getHandlerMethods().values().stream()
                .filter(handler -> handler.getBeanType().getPackageName()
                        .equals("com.pms.api.health.controller"))
                .toList();

        assertEquals(2, handlers.size());
        handlers.forEach(handler ->
                assertNotNull(handler.getMethodAnnotation(PreAuthorize.class),
                        handler.getMethod().getName() + " must declare @PreAuthorize."));
    }

    private MvcResult postVisit(String token, String body) throws Exception {
        var request = post(MEDICAL_VISITS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request).andReturn();
    }

    private User createUser(String role) {
        String username = "mv" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        return userRepository.saveAndFlush(
                new User("Medical Test Officer", username, "unused-password-hash", role, Set.of()));
    }

    private Inmate createInmate() {
        return inmateRepository.saveAndFlush(new Inmate(
                "MED-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20),
                "Medical", "Test", LocalDate.of(1990, 1, 1), "Other",
                LocalDate.of(2025, 1, 1)));
    }

    private String medicalVisitRequest(UUID inmateId, LocalDate visitDate, String sickness, String diagnosis)
            throws Exception {
        var request = objectMapper.createObjectNode();
        request.put("inmateId", inmateId.toString());
        request.put("visitDate", visitDate.toString());
        request.put("sickness", sickness);
        request.put("diagnosis", diagnosis);
        request.put("medicationsProvided", "Paracetamol");
        request.put("recommendations", "Review in 48 hours.");
        return objectMapper.writeValueAsString(request);
    }
}
