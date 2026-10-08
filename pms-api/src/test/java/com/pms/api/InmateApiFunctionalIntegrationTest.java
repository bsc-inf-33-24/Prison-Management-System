package com.pms.api;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
class InmateApiFunctionalIntegrationTest {

    private static final String INMATES_PATH = "/api/inmates";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private String recordsOfficerToken;

    @BeforeEach
    void setUp() {
        String username = "f" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
        User recordsOfficer = userRepository.saveAndFlush(
                new User("Functional Test", username, "unused-password-hash", "Records Officer", Set.of()));
        recordsOfficerToken = jwtService.generateToken(recordsOfficer);
    }

    @Test
    void enrollmentCreatesProfileAndIgnoresServerManagedFieldsInTheRequest() throws Exception {
        ObjectNode request = validRequest(uniqueInmateNumber(), "Jane", "Doe");
        request.put("id", 987654);
        request.put("currentFacility", "Client-controlled facility");
        request.put("createdAt", "2000-01-01T00:00:00Z");
        request.put("updatedAt", "2000-01-01T00:00:00Z");

        MvcResult result = mockMvc.perform(post(INMATES_PATH)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.currentFacility").value("Main Facility"))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotNull(response.path("id").longValue());
        assertTrue(response.path("id").longValue() != 987654);
        assertTrue(!response.path("createdAt").asText().startsWith("2000-"));
    }

    @Test
    void databaseUniqueConstraintReturnsConflictOnDuplicateInmateNumber() throws Exception {
        ObjectNode request = validRequest(uniqueInmateNumber(), "Unique", "Number");
        JsonNode existing = createInmate(request);

        mockMvc.perform(post(INMATES_PATH)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("An inmate with that inmate number already exists."));

        JsonNode anotherInmate = createInmate(validRequest(uniqueInmateNumber(), "Different", "Inmate"));
        ObjectNode duplicateUpdate = validRequest(request.path("inmateNumber").asText(), "Different", "Inmate");
        mockMvc.perform(put(INMATES_PATH + "/" + anotherInmate.path("id").asLong())
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUpdate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        assertNotNull(existing.path("id"));
    }

    @Test
    void updateChangesProfileAndNeverChangesCurrentFacilityOrCreatedAt() throws Exception {
        ObjectNode createRequest = validRequest(uniqueInmateNumber(), "Before", "Update");
        JsonNode created = createInmate(createRequest);
        String originalCreatedAt = created.path("createdAt").asText();
        String originalUpdatedAt = created.path("updatedAt").asText();
        long inmateId = created.path("id").asLong();

        ObjectNode updateRequest = validRequest(uniqueInmateNumber(), "After", "Update");
        updateRequest.put("id", 123456);
        updateRequest.put("currentFacility", "Another Facility");
        updateRequest.put("createdAt", "2001-01-01T00:00:00Z");
        updateRequest.put("updatedAt", "2001-01-01T00:00:00Z");

        mockMvc.perform(put(INMATES_PATH + "/" + inmateId)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("After"))
                .andExpect(jsonPath("$.currentFacility").value("Main Facility"))
                .andExpect(jsonPath("$.createdAt").value(originalCreatedAt))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(result -> {
                    JsonNode updated = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertNotEquals(originalUpdatedAt, updated.path("updatedAt").asText());
                    assertNotEquals("2001-01-01T00:00:00Z", updated.path("createdAt").asText());
                });
    }

    @Test
    void invalidAndOversizedRequestFieldsReturnStandardBadRequest() throws Exception {
        ObjectNode request = validRequest(uniqueInmateNumber(), "Invalid", "Request");
        request.put("firstName", "F".repeat(101));
        request.putNull("dateOfBirth");
        request.put("gender", "");

        mockMvc.perform(post(INMATES_PATH)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("One or more request fields are invalid."));
    }

    @Test
    void searchIsCaseInsensitiveEscapesWildcardsAndTreatsInjectionAsPlainText() throws Exception {
        createInmate(validRequest(uniqueInmateNumber(), "Percent%Name", "SearchCase"));
        createInmate(validRequest(uniqueInmateNumber(), "PercentXName", "SearchCase"));
        createInmate(validRequest(uniqueInmateNumber(), "Under_score", "SearchCase"));
        createInmate(validRequest(uniqueInmateNumber(), "Other", "SearchCase"));
        createInmate(validRequest(uniqueInmateNumber(), "Jane", "CaseInsensitive"));

        MvcResult percent = mockMvc.perform(get(INMATES_PATH)
                        .param("search", "%")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode percentResults = objectMapper.readTree(percent.getResponse().getContentAsString());
        assertTrue(percentResults.size() >= 1);
        assertTrue(containsFirstName(percentResults, "Percent%Name"));
        assertFalse(containsFirstName(percentResults, "PercentXName"));

        MvcResult underscore = mockMvc.perform(get(INMATES_PATH)
                        .param("search", "_")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode underscoreResults = objectMapper.readTree(underscore.getResponse().getContentAsString());
        assertTrue(containsFirstName(underscoreResults, "Under_score"));
        assertFalse(containsFirstName(underscoreResults, "PercentXName"));

        MvcResult injection = mockMvc.perform(get(INMATES_PATH)
                        .param("search", "' OR '1'='1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(objectMapper.readTree(injection.getResponse().getContentAsString()).isEmpty());

        MvcResult caseInsensitive = mockMvc.perform(get(INMATES_PATH)
                        .param("search", "jAnE")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(containsFirstName(
                objectMapper.readTree(caseInsensitive.getResponse().getContentAsString()), "Jane"));

        MvcResult all = mockMvc.perform(get(INMATES_PATH)
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(objectMapper.readTree(all.getResponse().getContentAsString()).size() >= 5);
    }

    @Test
    void allowedRoleGetsNotFoundForUnknownInmateId() throws Exception {
        mockMvc.perform(get(INMATES_PATH + "/999999999999")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Inmate not found."));
    }

    private JsonNode createInmate(ObjectNode request) throws Exception {
        MvcResult result = mockMvc.perform(post(INMATES_PATH)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private ObjectNode validRequest(String inmateNumber, String firstName, String lastName) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("inmateNumber", inmateNumber);
        request.put("firstName", firstName);
        request.put("lastName", lastName);
        request.put("dateOfBirth", LocalDate.of(1990, 1, 1).toString());
        request.put("gender", "Other");
        request.put("admissionDate", LocalDate.of(2025, 1, 1).toString());
        request.put("classification", "Minimum");
        return request;
    }

    private String uniqueInmateNumber() {
        return "IT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private boolean containsFirstName(JsonNode results, String name) {
        for (JsonNode inmate : results) {
            if (inmate.path("firstName").asText().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private String bearerToken() {
        return recordsOfficerToken;
    }
}
