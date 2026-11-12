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
import org.springframework.test.web.servlet.MvcResult;

// Covers incident creation and retrieval, role_in_incident preservation,
// optional outcome validation, and response behaviour.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IncidentApiFunctionalIntegrationTest {

    private static final String INCIDENTS_PATH = "/api/v1/incidents";

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

    private String disciplinaryOfficerToken;
    private String reportingOfficerFullName;
    private Prison testPrison;

    @BeforeEach
    void setUp() {
        testPrison = prisonRepository.saveAndFlush(new Prison(
                "I" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                "Incident Test Prison", null));
        reportingOfficerFullName = "Officer " + UUID.randomUUID().toString().substring(0, 8);
        User disciplinaryOfficer = new User(
                reportingOfficerFullName, uniqueUsername("d"), "unused-password-hash", "Disciplinary Officer", Set.of());
        disciplinaryOfficer.assignPrison(testPrison);
        disciplinaryOfficer = userRepository.saveAndFlush(disciplinaryOfficer);
        disciplinaryOfficerToken = jwtService.generateToken(disciplinaryOfficer);
    }

    @Test
    void logsIncidentPreservingRoleInIncidentAndDerivingReportedByFromTheToken() throws Exception {
        Inmate inmateA = createInmate();
        Inmate inmateB = createInmate();

        ObjectNode request = baseRequest("Fight broke out in the yard.");
        request.set("participants", participants(
                participant(inmateA.getPublicUuid(), "Instigator"),
                participant(inmateB.getPublicUuid(), "Bystander")));
        // Client tries to spoof the reporter - must be ignored, since
        // reported_by is derived from the authenticated JWT identity.
        request.put("reportedBy", "Someone Else");

        MvcResult result = mockMvc.perform(post(INCIDENTS_PATH)
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.status").value("open"))
                .andExpect(jsonPath("$.reportedBy").value(reportingOfficerFullName))
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(response.has("outcome"));
        assertTrue(response.path("outcome").isNull());
        assertTrue(hasParticipantWithRole(response, inmateA.getPublicUuid(), "Instigator"));
        assertTrue(hasParticipantWithRole(response, inmateB.getPublicUuid(), "Bystander"));
    }

    @Test
    void rejectsUnknownAndDuplicateInmateIds() throws Exception {
        Inmate inmate = createInmate();

        ObjectNode unknownInmateRequest = baseRequest("Unknown inmate test.");
        unknownInmateRequest.set("participants", participants(participant(UUID.randomUUID(), "Instigator")));
        mockMvc.perform(post(INCIDENTS_PATH)
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownInmateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        ObjectNode duplicateRequest = baseRequest("Duplicate inmate test.");
        duplicateRequest.set("participants", participants(
                participant(inmate.getPublicUuid(), "Instigator"),
                participant(inmate.getPublicUuid(), "Instigator again")));
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
        blankDescription.set("participants", participants(participant(inmate.getPublicUuid(), null)));
        mockMvc.perform(post(INCIDENTS_PATH)
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankDescription)))
                .andExpect(status().isBadRequest());

        ObjectNode futureDate = baseRequest("Future-dated incident.");
        futureDate.put("incidentDate", LocalDate.now().plusDays(1).toString());
        futureDate.set("participants", participants(participant(inmate.getPublicUuid(), null)));
        mockMvc.perform(post(INCIDENTS_PATH)
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(futureDate)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logsIncidentWithOutcomeAsResolved() throws Exception {
        Inmate inmate = createInmate();
        ObjectNode request = baseRequest("Incident with outcome.");
        request.set("participants", participants(participant(inmate.getPublicUuid(), "Role")));
        request.put("outcome", "Verbal warning issued.");

        mockMvc.perform(post(INCIDENTS_PATH)
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("resolved"))
                .andExpect(jsonPath("$.outcome").value("Verbal warning issued."));
    }

    @Test
    void acceptsExplicitNullOutcomeAsOpen() throws Exception {
        Inmate inmate = createInmate();
        ObjectNode request = baseRequest("Incident with null outcome.");
        request.set("participants", participants(participant(inmate.getPublicUuid(), "Role")));
        request.putNull("outcome");

        mockMvc.perform(post(INCIDENTS_PATH)
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("open"))
                .andExpect(jsonPath("$.outcome").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void acceptsOutcomeOf255Characters() throws Exception {
        Inmate inmate = createInmate();
        ObjectNode request = baseRequest("Incident with maximum-length outcome.");
        request.set("participants", participants(participant(inmate.getPublicUuid(), "Role")));
        request.put("outcome", "x".repeat(255));

        mockMvc.perform(post(INCIDENTS_PATH)
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("resolved"))
                .andExpect(jsonPath("$.outcome").value("x".repeat(255)));
    }

    @Test
    void rejectsEmptyOutcome() throws Exception {
        assertOutcomeRejected("");
    }

    @Test
    void rejectsWhitespaceOnlyOutcome() throws Exception {
        assertOutcomeRejected(" \t\n ");
    }

    @Test
    void rejectsOutcomeLongerThan255Characters() throws Exception {
        assertOutcomeRejected("x".repeat(256));
    }

    @Test
    void oldPatchOutcomeEndpointIsNoLongerAvailable() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("outcome", "No longer supported.");
        mockMvc.perform(patch(INCIDENTS_PATH + "/" + UUID.randomUUID() + "/outcome")
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void historyReturnsThatInmatesRoleAndOrdersMostRecentFirst() throws Exception {
        Inmate inmate = createInmate();
        logIncidentOn(LocalDate.of(2026, 1, 1), inmate.getPublicUuid(), "Witness");
        logIncidentOn(LocalDate.of(2026, 6, 1), inmate.getPublicUuid(), "Instigator");

        MvcResult result = mockMvc.perform(get("/api/v1/inmates/" + inmate.getPublicUuid() + "/incidents")
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
        mockMvc.perform(get("/api/v1/inmates/" + UUID.randomUUID() + "/incidents")
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private JsonNode logIncident(UUID inmateId, String role) throws Exception {
        return logIncidentOn(LocalDate.now(), inmateId, role);
    }

    private void assertOutcomeRejected(String outcome) throws Exception {
        Inmate inmate = createInmate();
        ObjectNode request = baseRequest("Invalid outcome test.");
        request.set("participants", participants(participant(inmate.getPublicUuid(), "Role")));
        request.put("outcome", outcome);
        mockMvc.perform(post(INCIDENTS_PATH)
                        .header("Authorization", "Bearer " + disciplinaryOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private JsonNode logIncidentOn(LocalDate date, UUID inmateId, String role) throws Exception {
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
        Inmate inmate = new Inmate(
                "Test", "Inmate", LocalDate.of(1990, 1, 1), "Other",
                LocalDate.of(2025, 1, 1), null, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1));
        inmate.assignPrison(testPrison);
        return inmateRepository.saveAndFlush(inmate);
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

    private ObjectNode participant(UUID inmateId, String role) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("inmateId", inmateId.toString());
        if (role != null) {
            node.put("roleInIncident", role);
        }
        return node;
    }

    private boolean hasParticipantWithRole(JsonNode response, UUID inmateId, String role) {
        for (JsonNode participant : response.path("participants")) {
            if (inmateId.toString().equals(participant.path("inmateId").asText())
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
