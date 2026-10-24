package com.pms.api;

import com.pms.api.common.audit.AuditLogRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.pms.api.security.jwt.JwtService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HealthApiSecurityIntegrationTest {

    private static final List<String> HEALTH_ENDPOINTS = List.of(
            "/api/v1/health-records",
            "/api/v1/medical-conditions",
            "/api/v1/allergies",
            "/api/v1/consultations",
            "/api/v1/medications",
            "/api/v1/appointments"
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void medicalOfficerCanAccessAllHealthLists() throws Exception {
        String token = tokenForRole("Medical Officer");
        for (String endpoint : HEALTH_ENDPOINTS) {
            expectStatus(token, endpoint, 200);
        }
    }

    @Test
    void otherRolesCannotAccessHealthLists() throws Exception {
        for (String role : User.ALLOWED_ROLES) {
            if (role.equals("Medical Officer")) {
                continue;
            }
            String token = tokenForRole(role);
            for (String endpoint : HEALTH_ENDPOINTS) {
                expectStatus(token, endpoint, 403);
            }
        }
    }

    @Test
    void missingAndInvalidTokensAreRejected() throws Exception {
        for (String endpoint : HEALTH_ENDPOINTS) {
            expectStatus(null, endpoint, 401);
            expectStatus("not-a-valid-jwt", endpoint, 401);
        }
    }

    @Test
    void deniedHealthAccessCreatesAuditRecord() throws Exception {
        String role = User.ALLOWED_ROLES.stream()
                .filter(candidate -> !candidate.equals("Medical Officer"))
                .findFirst()
                .orElseThrow();

        String token = tokenForRole(role);
        long recordsBefore = auditLogRepository.count();

        expectStatus(token, "/api/v1/health-records", 403);

        long recordsAfter = auditLogRepository.count();
        org.junit.jupiter.api.Assertions.assertTrue(
                recordsAfter > recordsBefore,
                "Denied health access should create a security audit record.");
    }

    private void expectStatus(String token, String endpoint, int expectedStatus)
            throws Exception {
        MockHttpServletRequestBuilder request = get(endpoint);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        int actualStatus = mockMvc.perform(request)
                .andReturn().getResponse().getStatus();
        assertEquals(expectedStatus, actualStatus, endpoint);
    }

    private String tokenForRole(String role) {
        String username = "health" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 14);
        User user = userRepository.saveAndFlush(
                new User("Health Security Test", username,
                        "unused-password-hash", role, Set.of()));
        return jwtService.generateToken(user);
    }
}
