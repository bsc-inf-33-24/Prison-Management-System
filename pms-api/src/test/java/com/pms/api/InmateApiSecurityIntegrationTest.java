package com.pms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pms.api.security.jwt.JwtService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InmateApiSecurityIntegrationTest {

    private static final String INMATES_PATH = "/api/inmates";
    private static final String UNKNOWN_INMATE_PATH = INMATES_PATH + "/999999999999";
    private static final Set<String> API_ERROR_FIELDS = Set.of("status", "error", "message");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void appliesRoleMatrixAndChecksRoleBeforeLookingUpInmateIds() throws Exception {
        expectStatus(tokenForRole("Records Officer"), "POST", INMATES_PATH, validRequest(), 201);
        String recordsOfficerToken = tokenForRole("Records Officer");
        expectStatus(recordsOfficerToken, "GET", INMATES_PATH, null, 200);
        expectStatus(recordsOfficerToken, "GET", UNKNOWN_INMATE_PATH, null, 404);
        expectStatus(recordsOfficerToken, "PUT", UNKNOWN_INMATE_PATH, validRequest(), 404);

        String superAdminToken = tokenForRole("Super Admin");
        expectStatus(superAdminToken, "POST", INMATES_PATH, validRequest(), 403);
        expectStatus(superAdminToken, "GET", INMATES_PATH, null, 200);
        expectStatus(superAdminToken, "GET", UNKNOWN_INMATE_PATH, null, 404);
        expectStatus(superAdminToken, "PUT", UNKNOWN_INMATE_PATH, validRequest(), 403);

        for (String role : User.ALLOWED_ROLES) {
            if (role.equals("Records Officer") || role.equals("Super Admin")) {
                continue;
            }
            String token = tokenForRole(role);
            expectStatus(token, "POST", INMATES_PATH, validRequest(), 403);
            expectStatus(token, "GET", INMATES_PATH, null, 403);
            expectStatus(token, "GET", UNKNOWN_INMATE_PATH, null, 403);
            expectStatus(token, "PUT", UNKNOWN_INMATE_PATH, validRequest(), 403);
        }
    }

    @Test
    void rejectsMissingInvalidAndExpiredTokensOnEveryEndpoint() throws Exception {
        expectEveryEndpointStatus(null, 401);
        expectEveryEndpointStatus("not-a-valid-jwt", 401);
        expectEveryEndpointStatus(expiredToken(), 401);
    }

    @Test
    void rejectsTokenAfterAccountDeactivation() throws Exception {
        User user = createUser("Records Officer");
        String token = jwtService.generateToken(user);
        user.deactivate();
        userRepository.saveAndFlush(user);
        expectEveryEndpointStatus(token, 401);
    }

    @Test
    void rejectsTokenAfterAccountLockout() throws Exception {
        User user = createUser("Records Officer");
        String token = jwtService.generateToken(user);
        user.recordFailedLogin(1, Instant.now().plusSeconds(600));
        userRepository.saveAndFlush(user);
        expectEveryEndpointStatus(token, 401);
    }

    @Test
    void everyInmateControllerMethodDeclaresPreAuthorize() {
        var inmateHandlers = handlerMapping.getHandlerMethods().values().stream()
                .filter(handler -> handler.getBeanType().getPackageName()
                        .equals("com.pms.api.inmate.controller"))
                .toList();

        assertEquals(4, inmateHandlers.size());
        inmateHandlers.forEach(handler ->
                assertNotNull(handler.getMethodAnnotation(PreAuthorize.class),
                        handler.getMethod().getName() + " must declare @PreAuthorize."));
    }

    private void expectEveryEndpointStatus(String token, int expectedStatus) throws Exception {
        expectStatus(token, "POST", INMATES_PATH, validRequest(), expectedStatus);
        expectStatus(token, "GET", INMATES_PATH, null, expectedStatus);
        expectStatus(token, "GET", UNKNOWN_INMATE_PATH, null, expectedStatus);
        expectStatus(token, "PUT", UNKNOWN_INMATE_PATH, validRequest(), expectedStatus);
    }

    private void expectStatus(String token, String method, String path, String body, int expectedStatus)
            throws Exception {
        MockHttpServletRequestBuilder request = switch (method) {
            case "POST" -> post(path);
            case "GET" -> get(path);
            case "PUT" -> put(path);
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
        String username = "s" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
        return userRepository.saveAndFlush(new User("Security Test", username, "unused-password-hash", role, Set.of()));
    }

    private String validRequest() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("inmateNumber", "SECURITY-" + UUID.randomUUID().toString().replace("-", ""));
        request.put("firstName", "Test");
        request.put("lastName", "Inmate");
        request.put("dateOfBirth", "1990-01-01");
        request.put("gender", "Other");
        request.put("admissionDate", "2025-01-01");
        request.put("classification", "Minimum");
        try {
            return objectMapper.writeValueAsString(request);
        } catch (java.io.IOException exception) {
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
