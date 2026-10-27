package com.pms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.security.jwt.JwtService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

// Covers Issue #27's functional acceptance criteria: incident creation and
// retrieval, role_in_incident preservation, outcome recording (including the
// write-once / concurrency-safe behaviour), and validation.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IncidentApiFunctionalIntegrationTest {

    private static final String INCIDENTS_PATH = "/api/incidents";

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

    private String disciplinaryOfficerToken;
    private String reportingOfficerFullName;
    private String superAdminToken;

    @BeforeEach
    void setUp() {
        reportingOfficerFullName = "Officer " + UUID.randomUUID().toString().substring(0, 8);
        User disciplinaryOfficer = userRepository.saveAndFlush(new User(
                reportingOfficerFullName, uniqueUsername("d"), "unused-password-hash", "Disciplinary Officer", Set.of()));
        disciplinaryOfficerToken = jwtService.generateToken(disciplinaryOfficer);

        User superAdmin = userRepository.saveAndFlush(
                new User("Admin Test", uniqueUsername("a"), "unused-password-hash", "Super Admin", Set.of()));
        superAdminToken = jwtService.generateToken(superAdmin);
    }

    @Test
    void logsIncidentPreservingRoleInIncidentAndDerivingReportedByFromTheToken() throws Exception {
        Inmate inmateA = createInmate();
        Inmate inmateB = createInmate();

        ObjectNode request = baseRequest("Fight broke out in the yard.");
        request.set("participants", participants(
                participant(inmateA.getId(), "Instigator"),
                participant(inmateB.getId(), "Bystander")));
        // Client tries to spoof the reporter - must be ignored, since
        // reported_by is derived from the authenticated JWT identity.
        request.put("reportedBy", "Someone Else");

        MvcResult result = mockMvc.perform(post(INCIDENTS_PATH)
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("open"))
                .andExpect(jsonPath("$.outcome").doesNotExist())
                .andExpect(jsonPath("$.reportedBy").value(reportingOfficerFullName))
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(hasParticipantWithRole(response, inmateA.getId(), "Instigator"));
        assertTrue(hasParticipantWithRole(response, inmateB.getId(), "Bystander"));
    }

    @Test
    void rejectsUnknownAndDuplicateInmateIds() throws Exception {
        Inmate inmate = createInmate();

        ObjectNode unknownInmateRequest = baseRequest("Unknown inmate test.");
        unknownInmateRequest.set("participants", participants(participant(999999999L, "Instigator")));
        mockMvc.perform(post(INCIDENTS_PATH)
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownInmateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        ObjectNode duplicateRequest = baseRequest("Duplicate inmate test.");
        duplicateRequest.set("participants", participants(
                participant(inmate.getId(), "Instigator"),
                participant(inmate.getId(), "Instigator again")));
        mockMvc.perform(post(INCIDENTS_PATH)
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsMalformedRequestsWithBadRequest() throws Exception {
        ObjectNode emptyParticipants = baseRequest("No participants.");
        emptyParticipants.set("participants", objectMapper.createArrayNode());
        mockMvc.perform(post(INCIDENTS_PATH)
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyParticipants)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        Inmate inmate = createInmate();
        ObjectNode blankDescription = baseRequest("");
        blankDescription.set("participants", participants(participant(inmate.getId(), null)));
        mockMvc.perform(post(INCIDENTS_PATH)
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankDescription)))
                .andExpect(status().isBadRequest());

        ObjectNode futureDate = baseRequest("Future-dated incident.");
        futureDate.put("incidentDate", LocalDate.now().plusDays(1).toString());
        futureDate.set("participants", participants(participant(inmate.getId(), null)));
        mockMvc.perform(post(INCIDENTS_PATH)
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(futureDate)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordsOutcomeOnceAndRejectsASecondAttemptWithConflict() throws Exception {
        Inmate inmate = createInmate();
        JsonNode created = logIncident(inmate.getId(), "Role");
        long incidentId = created.path("id").asLong();

        ObjectNode outcomeRequest = objectMapper.createObjectNode();
        outcomeRequest.put("outcome", "Verbal warning issued.");

        mockMvc.perform(patch(INCIDENTS_PATH + "/" + incidentId + "/outcome")
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(outcomeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("resolved"))
                .andExpect(jsonPath("$.outcome").value("Verbal warning issued."));

        mockMvc.perform(patch(INCIDENTS_PATH + "/" + incidentId + "/outcome")
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(outcomeRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void outcomeOnUnknownIncidentReturnsNotFound() throws Exception {
        ObjectNode outcomeRequest = objectMapper.createObjectNode();
        outcomeRequest.put("outcome", "N/A");

        mockMvc.perform(patch(INCIDENTS_PATH + "/999999999/outcome")
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(outcomeRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void concurrentOutcomeAttemptsCannotBothSucceed() throws Exception {
        Inmate inmate = createInmate();
        JsonNode created = logIncident(inmate.getId(), "Role");
        long incidentId = created.path("id").asLong();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try {
            Callable<Integer> attempt = () -> {
                barrier.await();
                ObjectNode outcomeRequest = objectMapper.createObjectNode();
                outcomeRequest.put("outcome", "Racing outcome " + Thread.currentThread().getId());
                return mockMvc.perform(patch(INCIDENTS_PATH + "/" + incidentId + "/outcome")
                                .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(outcomeRequest)))
                        .andReturn().getResponse().getStatus();
            };

            Future<Integer> first = executor.submit(attempt);
            Future<Integer> second = executor.submit(attempt);
            List<Integer> results = List.of(first.get(), second.get());

            assertEquals(1, results.stream().filter(status -> status == 200).count(),
                    "exactly one concurrent outcome attempt should succeed");
            assertEquals(1, results.stream().filter(status -> status == 409).count(),
                    "exactly one concurrent outcome attempt should be rejected as a conflict");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void historyReturnsThatInmatesRoleAndOrdersMostRecentFirst() throws Exception {
        Inmate inmate = createInmate();
        logIncidentOn(LocalDate.of(2026, 1, 1), inmate.getId(), "Witness");
        logIncidentOn(LocalDate.of(2026, 6, 1), inmate.getId(), "Instigator");

        MvcResult result = mockMvc.perform(get("/api/inmates/" + inmate.getId() + "/incidents")
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode history = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(2, history.size());
        assertEquals("2026-06-01", history.get(0).path("incidentDate").asText());
        assertEquals("Instigator", history.get(0).path("roleInIncident").asText());
        assertEquals("2026-01-01", history.get(1).path("incidentDate").asText());
        assertEquals("Witness", history.get(1).path("roleInIncident").asText());
    }

    @Test
    void historyForUnknownInmateReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/inmates/999999999/incidents")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private JsonNode logIncident(Long inmateId, String role) throws Exception {
        return logIncidentOn(LocalDate.now(), inmateId, role);
    }

    private JsonNode logIncidentOn(LocalDate date, Long inmateId, String role) throws Exception {
        ObjectNode request = baseRequest("Incident on " + date);
        request.put("incidentDate", date.toString());
        request.set("participants", participants(participant(inmateId, role)));

        MvcResult result = mockMvc.perform(post(INCIDENTS_PATH)
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Inmate createInmate() {
        return inmateRepository.saveAndFlush(new Inmate(
                "Test", "Inmate", LocalDate.of(1990, 1, 1), "Other",
                LocalDate.of(2025, 1, 1), null, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1)));
    }

    private ObjectNode baseRequest(String description) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("incidentDate", LocalDate.now().toString());
        request.put("description", description);
        return request;
    }

    private ArrayNode participants(ObjectNode... entries) {
        ArrayNode array = objectMapper.createArrayNode();
        for (ObjectNode entry : entries) {
            array.add(entry);
        }
        return array;
    }

    private ObjectNode participant(Long inmateId, String role) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("inmateId", inmateId);
        if (role != null) {
            node.put("roleInIncident", role);
        }
        return node;
    }

    private boolean hasParticipantWithRole(JsonNode response, Long inmateId, String role) {
        for (JsonNode participant : response.path("participants")) {
            if (participant.path("inmateId").asLong() == inmateId
                    && role.equals(participant.path("roleInIncident").asText())) {
                return true;
            }
        }
        return false;
    }

    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
    }
}
