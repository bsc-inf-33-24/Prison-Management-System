package com.pms.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pms.api.auth.repository.PasswordResetTokenRepository;
import com.pms.api.auth.service.AuthService;
import com.pms.api.auth.service.RecoveryTokenDelivery;
import com.pms.api.security.jwt.JwtService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.util.Optional;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserEmailAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @MockitoBean
    private RecoveryTokenDelivery recoveryDelivery;

    @BeforeEach
    void resetRecoveryDeliveryMock() {
        reset(recoveryDelivery);
    }

    @Test
    void acceptsOptionalEmailAndAuthenticatesWithUsernameOrNormalizedEmail() throws Exception {
        String noEmailUsername = uniqueUsername();
        JsonNode noEmailResponse = createUser(noEmailUsername, null, false);
        assertTrue(noEmailResponse.path("email").isNull());
        login(noEmailUsername, "Records123!").andExpect(status().isOk());

        String username = uniqueUsername();
        JsonNode response = createUser(username, "  Records." + UUID.randomUUID() + "@Example.Test  ", true);
        String email = response.path("email").asText();
        assertEquals(email.toLowerCase(java.util.Locale.ROOT), email);
        assertFalse(response.has("password"));
        assertFalse(response.has("passwordHash"));

        login(username, "Records123!").andExpect(status().isOk());
        loginByUsername(username, "Records123!").andExpect(status().isOk());
        loginByEmail(email, "Records123!").andExpect(status().isOk());
        login(email, "Records123!").andExpect(status().isOk());
        login(email.toUpperCase(java.util.Locale.ROOT), "Records123!").andExpect(status().isOk());
        login("  " + email.toUpperCase(java.util.Locale.ROOT) + "  ", "Records123!")
                .andExpect(status().isOk());
    }

    @Test
    void rejectsDuplicateInvalidBlankAndOversizedEmails() throws Exception {
        String email = "duplicate." + UUID.randomUUID() + "@example.test";
        String username = uniqueUsername();
        createUser(username, email, true);

        createUserRequest(username, "duplicate.username." + UUID.randomUUID() + "@example.test", true)
            .andExpect(status().isConflict());
        createUserRequest(uniqueUsername(), email.toUpperCase(java.util.Locale.ROOT), true)
                .andExpect(status().isConflict());
        createUserRequest(uniqueUsername(), "not-an-email", true).andExpect(status().isBadRequest());
        createUserRequest(uniqueUsername(), "   ", true).andExpect(status().isBadRequest());
        createUserRequest(uniqueUsername(), "a".repeat(245) + "@example.test", true)
                .andExpect(status().isBadRequest());
        createUser(uniqueUsername(), null, true);
    }

    @Test
    void userCreationValidatesRequiredFieldsRolesAndSuperAdminAuthorization() throws Exception {
        ObjectNode incompleteRequest = objectMapper.createObjectNode();
        incompleteRequest.put("username", uniqueUsername());
        incompleteRequest.put("password", "Records123!");
        incompleteRequest.put("role", "Records Officer");
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incompleteRequest)))
                .andExpect(status().isBadRequest());

        ObjectNode weakPasswordRequest = objectMapper.createObjectNode();
        weakPasswordRequest.put("fullName", "Test User");
        weakPasswordRequest.put("username", uniqueUsername());
        weakPasswordRequest.put("password", "weak");
        weakPasswordRequest.put("role", "Records Officer");
        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(weakPasswordRequest)))
            .andExpect(status().isBadRequest());

        ObjectNode invalidRoleRequest = objectMapper.createObjectNode();
        invalidRoleRequest.put("fullName", "Test User");
        invalidRoleRequest.put("username", uniqueUsername());
        invalidRoleRequest.put("password", "Records123!");
        invalidRoleRequest.put("role", "Prisoner");
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRoleRequest)))
                .andExpect(status().isBadRequest());

        User recordsOfficer = userRepository.saveAndFlush(
                new User("Test Records Officer", uniqueUsername(), "unused-password-hash",
                        "Records Officer", Set.of()));
        ObjectNode validRequest = objectMapper.createObjectNode();
        validRequest.put("fullName", "Test User");
        validRequest.put("username", uniqueUsername());
        validRequest.put("password", "Records123!");
        validRequest.put("role", "Records Officer");
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + jwtService.generateToken(recordsOfficer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownWrongPasswordAndInactiveEmailLoginsHaveSafeFailures() throws Exception {
        String username = uniqueUsername();
        JsonNode created = createUser(username, "inactive." + UUID.randomUUID() + "@example.test", true);
        String email = created.path("email").asText();

        String wrongPassword = login(username, "Wrong123!").andExpect(status().isUnauthorized()).andReturn()
                .getResponse().getContentAsString();
        MvcResult unknownEmail = login("missing." + UUID.randomUUID() + "@example.test", "Records123!")
            .andReturn();
        assertEquals(401, unknownEmail.getResponse().getStatus(),
            unknownEmail.getResponse().getContentAsString());
        assertEquals(wrongPassword, unknownEmail.getResponse().getContentAsString());
        MvcResult unknownUsername = login(uniqueUsername(), "Records123!")
            .andExpect(status().isUnauthorized()).andReturn();
        assertEquals(wrongPassword, unknownUsername.getResponse().getContentAsString());
        login(email, "Wrong123!").andExpect(status().isUnauthorized());

        User user = userRepository.findByUsername(username).orElseThrow();
        MvcResult deactivated = mockMvc.perform(patch("/api/v1/users/{id}/deactivate", user.getPublicUuid())
                        .header("Authorization", "Bearer " + adminToken())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        assertFalse(objectMapper.readTree(deactivated.getResponse().getContentAsString()).has("passwordHash"));

        String inactiveUsernameResponse = login(username, "Records123!")
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String inactiveEmailResponse = login(email, "Records123!")
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertEquals(inactiveUsernameResponse, inactiveEmailResponse);
    }

    @Test
    void emailFailuresShareUsernameLockoutAndPasswordChangeBehavior() throws Exception {
        String username = uniqueUsername();
        String email = "lockout." + UUID.randomUUID() + "@example.test";
        createUser(username, email, true);

        for (int attempt = 0; attempt < 3; attempt++) {
            login(email.toUpperCase(java.util.Locale.ROOT), "Wrong123!").andExpect(status().isUnauthorized());
        }
        login(email, "Records123!").andExpect(status().isTooManyRequests());
        login(username, "Records123!").andExpect(status().isTooManyRequests());

        User user = userRepository.findByUsername(username).orElseThrow();
        user.resetLoginFailures();
        userRepository.saveAndFlush(user);
        String loginResponse = login(email, "Records123!").andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        String bearerToken = objectMapper.readTree(loginResponse).path("accessToken").asText();
        ObjectNode changePassword = objectMapper.createObjectNode();
        changePassword.put("currentPassword", "Records123!");
        changePassword.put("newPassword", "Records456!");
        mockMvc.perform(post("/api/v1/auth/password/change")
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePassword)))
                .andExpect(status().isNoContent());
        login(username, "Records456!").andExpect(status().isOk());
        login(email, "Records456!").andExpect(status().isOk());
        login(email, "Records123!").andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void recoversByEmailWithoutDisclosingWhetherTheAddressIsRegistered() throws Exception {
        String username = uniqueUsername();
        String email = "recovery." + UUID.randomUUID() + "@example.test";
        createUser(username, " " + email + " ", true);

        ObjectNode knownRequest = objectMapper.createObjectNode();
        knownRequest.put("username", "  " + email.toUpperCase(java.util.Locale.ROOT) + "  ");
        MvcResult knownResult = mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(knownRequest)))
                .andExpect(status().isAccepted()).andReturn();
        String knownResponse = knownResult.getResponse().getContentAsString();
        assertFalse(knownResponse.contains(email));

        ArgumentCaptor<String> rawToken = ArgumentCaptor.forClass(String.class);
        verify(recoveryDelivery).deliver(eq(Optional.of(email)), rawToken.capture());
        String firstToken = rawToken.getValue();
        assertTrue(resetTokenRepository.findByTokenHash(AuthService.hash(firstToken)).isPresent());

        reset(recoveryDelivery);
        MvcResult resendResult = mockMvc.perform(post("/api/v1/auth/password/forgot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(knownRequest)))
            .andExpect(status().isAccepted()).andReturn();
        assertEquals(knownResponse, resendResult.getResponse().getContentAsString());
        ArgumentCaptor<String> resentToken = ArgumentCaptor.forClass(String.class);
        verify(recoveryDelivery).deliver(eq(Optional.of(email)), resentToken.capture());
        assertFalse(resetTokenRepository.findByTokenHash(AuthService.hash(firstToken)).isPresent());
        assertTrue(resetTokenRepository.findByTokenHash(AuthService.hash(resentToken.getValue())).isPresent());

        reset(recoveryDelivery);
        ObjectNode unknownRequest = objectMapper.createObjectNode();
        unknownRequest.put("username", "missing." + UUID.randomUUID() + "@example.test");
        MvcResult unknownResult = mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownRequest)))
                .andExpect(status().isAccepted()).andReturn();
        assertEquals(knownResponse, unknownResult.getResponse().getContentAsString());
        verify(recoveryDelivery).deliver(eq(Optional.empty()), anyString());
    }

    private JsonNode createUser(String username, String email, boolean includeEmail) throws Exception {
        MvcResult result = createUserRequest(username, email, includeEmail)
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private ResultActions createUserRequest(String username, String email, boolean includeEmail) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("fullName", "Test Records Officer");
        request.put("username", username);
        request.put("password", "Records123!");
        request.put("role", "Records Officer");
        if (includeEmail) {
            if (email == null) {
                request.putNull("email");
            } else {
                request.put("email", email);
            }
        }
        return mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions login(String login, String password) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("identifier", login);
        request.put("password", password);
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions loginByUsername(String username, String password) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("username", username);
        request.put("password", password);
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions loginByEmail(String email, String password) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("email", email);
        request.put("password", password);
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private String adminToken() {
        String username = uniqueUsername();
        User admin = userRepository.saveAndFlush(
                new User("Test Admin", username, "unused-password-hash", "Super Admin", Set.of()));
        return jwtService.generateToken(admin);
    }

    private String uniqueUsername() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
    }
}
