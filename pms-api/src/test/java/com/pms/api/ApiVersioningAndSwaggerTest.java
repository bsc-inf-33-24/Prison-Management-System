package com.pms.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ApiVersioningAndSwaggerTest {

    @Autowired
    private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

    @Test
    void openApiUsesCanonicalVersionedBusinessPaths() throws Exception {
        String docs = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(docs)
                .contains("/api/v1/auth/login")
                .contains("/api/v1/users")
                .contains("/api/v1/inmates")
                .contains("/api/v1/cells")
                .contains("/api/v1/cases")
                .contains("/api/v1/sentences")
                .contains("/api/v1/incidents")
                .contains("/api/v1/rehabilitation/programs")
                .contains("/api/v1/rehabilitation/enrollments")
                .contains("/api/v1/rehabilitation/attendance");

        assertThat(docs)
                .doesNotContain("\"/api/inmates\"")
                .doesNotContain("\"/api/cells\"")
                .doesNotContain("\"/api/incidents\"")
                .doesNotContain("\"/api/sentences\"");

        JsonNode document = objectMapper.readTree(docs);
        JsonNode createUserSchema = document.at("/components/schemas/CreateUserRequest");
        assertThat(createUserSchema.at("/properties/email").isObject()).isTrue();
        assertThat(createUserSchema.path("required").toString()).doesNotContain("email");
        assertThat(createUserSchema.at("/properties/email/description").asText())
                .contains("Optional", "Omit", "valid");
        assertThat(document.at("/paths/~1api~1v1~1users/post/requestBody/content/application~1json/examples/User with email/value/email").asText())
                .isEqualTo("records.officer@example.test");
        JsonNode userChoices = document.at("/paths/~1api~1v1~1users/post/requestBody/content/application~1json/schema/oneOf");
        assertThat(userChoices.toString()).contains("CreateUserWithEmailRequest", "CreateUserWithoutEmailRequest");
        assertThat(document.at("/paths/~1api~1v1~1users/post/requestBody/content/application~1json/examples/User without email/value/email").isMissingNode())
                .isTrue();
        JsonNode authSchema = document.at("/components/schemas/AuthRequest");
        assertThat(authSchema.at("/properties/email/description").asText())
                .contains("Optional", "email", "username");
        assertThat(authSchema.path("required").toString()).doesNotContain("email");
        assertThat(document.at("/paths/~1api~1v1~1auth~1login/post/requestBody/content/application~1json/examples/Log in with email/value/email").asText())
                .isEqualTo("records.officer@example.test");
        JsonNode loginChoices = document.at("/paths/~1api~1v1~1auth~1login/post/requestBody/content/application~1json/schema/oneOf");
        assertThat(loginChoices.toString()).contains("UsernameLoginRequest", "EmailLoginRequest");
    }
}
