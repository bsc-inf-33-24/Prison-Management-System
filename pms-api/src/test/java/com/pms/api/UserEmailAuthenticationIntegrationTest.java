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
import com.pms.api.security.jwt.JwtService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

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
        login(email, "Records123!").andExpect(status().isOk());
        login(email.toUpperCase(java.util.Locale.ROOT), "Records123!").andExpect(status().isOk());
        login("  " + email.toUpperCase(java.util.Locale.ROOT) + "  ", "Records123!")
                .andExpect(status().isOk());
    }

    @Test
    void rejectsDuplicateInvalidBlankAndOversizedEmails() throws Exception {
        String email = "duplicate." + UUID.randomUUID() + "@example.test";
        createUser(uniqueUsername(), email, true);

        createUserRequest(uniqueUsername(), email.toUpperCase(java.util.Locale.ROOT), true)
                .andExpect(status().isConflict());
        createUserRequest(uniqueUsername(), "not-an-email", true).andExpect(status().isBadRequest());
        createUserRequest(uniqueUsername(), "   ", true).andExpect(status().isBadRequest());
        createUserRequest(uniqueUsername(), "a".repeat(245) + "@example.test", true)
                .andExpect(status().isBadRequest());
        createUser(uniqueUsername(), null, true);
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
        request.put("username", login);
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