package com.pms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.security.jwt.JwtService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// Covers Issue #27's security acceptance criteria for the Incident endpoints:
// role matrix, missing/invalid/expired JWTs, and that every handler method
// declares @PreAuthorize.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IncidentApiSecurityIntegrationTest {

    private static final String INCIDENTS_PATH = "/api/v1/incidents";
    private static final Set<String> API_ERROR_FIELDS = Set.of("status", "error", "message");

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

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void onlyDisciplinaryOfficerCanLogIncidentsAndRecordOutcomes() throws Exception {
        Inmate inmate = createInmate();
        String historyPath = "/api/v1/inmates/" + inmate.getPublicUuid() + "/incidents";

        String disciplinaryOfficerToken = tokenForRole("Disciplinary Officer");
        expectStatus(disciplinaryOfficerToken, "POST", INCIDENTS_PATH, incidentRequest(inmate.getPublicUuid()), 201);
        expectStatus(disciplinaryOfficerToken, "GET", historyPath, null, 200);

        String superAdminToken = tokenForRole("Super Admin");
        // Super Admin may read history, but not log incidents or record outcomes.
        expectStatus(superAdminToken, "GET", historyPath, null, 200);
        expectStatus(superAdminToken, "POST", INCIDENTS_PATH, incidentRequest(inmate.getPublicUuid()), 403);

        for (String role : User.ALLOWED_ROLES) {
            if (role.equals("Disciplinary Officer") || role.equals("Super Admin")) {
                continue;
            }
            String token = tokenForRole(role);
            expectStatus(token, "POST", INCIDENTS_PATH, incidentRequest(inmate.getPublicUuid()), 403);
            expectStatus(token, "GET", historyPath, null, 403);
        }
    }

    @Test
    void rejectsMissingInvalidAndExpiredTokensOnEveryEndpoint() throws Exception {
        Inmate inmate = createInmate();
        String historyPath = "/api/v1/inmates/" + inmate.getPublicUuid() + "/incidents";
        String outcomePath = INCIDENTS_PATH + "/1/outcome";

        for (String token : new String[] { null, "not-a-valid-jwt", expiredToken() }) {
            expectStatus(token, "POST", INCIDENTS_PATH, incidentRequest(inmate.getPublicUuid()), 401);
            expectStatus(token, "GET", historyPath, null, 401);
            expectStatus(token, "PATCH", outcomePath, outcomeRequest(), 401);
        }
    }

    @Test
    void rejectsTokenAfterAccountDeactivation() throws Exception {
        User user = createUser("Disciplinary Officer");
        String token = jwtService.generateToken(user);
        user.deactivate();
        userRepository.saveAndFlush(user);

        Inmate inmate = createInmate();
        expectStatus(token, "POST", INCIDENTS_PATH, incidentRequest(inmate.getPublicUuid()), 401);
    }

    @Test
    void everyIncidentControllerMethodDeclaresPreAuthorize() {
        var incidentHandlers = handlerMapping.getHandlerMethods().values().stream()
                .filter(handler -> handler.getBeanType().getPackageName()
                        .equals("com.pms.api.incident.controller"))
                .toList();

        assertEquals(3, incidentHandlers.size());
        incidentHandlers.forEach(handler ->
                assertNotNull(handler.getMethodAnnotation(PreAuthorize.class),
                        handler.getMethod().getName() + " must declare @PreAuthorize."));
    }

    private void expectStatus(String token, String method, String path, String body, int expectedStatus)
            throws Exception {
        MockHttpServletRequestBuilder request = switch (method) {
            case "POST" -> post(path);
            case "GET" -> get(path);
            case "PATCH" -> patch(path);
            default -> throw new IllegalArgumentException("Unsupported test HTTP method.");
        };
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }

        MvcResult result = mockMvc.perform(request).andReturn();
        assertEquals(expectedStatus, result.getResponse().getStatus(), method + " " + path);
        if (expectedStatus >= 400) {
            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            Set<String> responseFields = new HashSet<>();
            response.fieldNames().forEachRemaining(responseFields::add);
            assertEquals(API_ERROR_FIELDS, responseFields);
            assertEquals(expectedStatus, response.path("status").asInt());
            assertTrue(response.path("error").isTextual());
            assertTrue(response.path("message").isTextual());
        }
    }

    private String tokenForRole(String role) {
        return jwtService.generateToken(createUser(role));
    }

    private User createUser(String role) {
        String username = "si" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        return userRepository.saveAndFlush(new User("Security Test", username, "unused-password-hash", role, Set.of()));
    }

    private Inmate createInmate() {
        return inmateRepository.saveAndFlush(new Inmate(
                "SEC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20),
                "Security", "Test", LocalDate.of(1990, 1, 1), "Other",
                LocalDate.of(2025, 1, 1)));
    }

    private String incidentRequest(UUID inmateId) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("incidentDate", LocalDate.now().toString());
        request.put("description", "Security test incident.");
        ArrayNode participants = objectMapper.createArrayNode();
        ObjectNode participant = objectMapper.createObjectNode();
        participant.put("inmateId", inmateId.toString());
        participant.put("roleInIncident", "Involved");
        participants.add(participant);
        request.set("participants", participants);
        try {
            return objectMapper.writeValueAsString(request);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not serialize test request.", exception);
        }
    }

    private String outcomeRequest() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("outcome", "Security test outcome.");
        try {
            return objectMapper.writeValueAsString(request);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not serialize test request.", exception);
        }
    }

    private String expiredToken() {
        Instant issuedAt = Instant.now().minusSeconds(600);
        return Jwts.builder()
                .subject("expired-test-user")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
