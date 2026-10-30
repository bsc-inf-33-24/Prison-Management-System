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
                .contains("/api/v1/incidents")
                .contains("/api/v1/programs")
                .contains("/api/v1/programs/enrollments")
                .contains("/api/v1/programs/attendance");

        assertThat(docs)
                .doesNotContain("\"/api/inmates\"")
                .doesNotContain("\"/api/cells\"")
                .doesNotContain("\"/api/incidents\"")
                .doesNotContain("/api/v1/rehabilitation/programs")
                .doesNotContain("/api/v1/cases")
                .doesNotContain("/api/v1/sentences");

        JsonNode document = objectMapper.readTree(docs);
        JsonNode createUserSchema = document.at("/components/schemas/CreateUserRequest");
        assertThat(createUserSchema.at("/properties/email").isObject()).isTrue();
        assertThat(createUserSchema.path("required").toString()).contains("fullName", "username", "password", "role")
                .doesNotContain("email");
        assertThat(createUserSchema.at("/properties/password/minLength").asInt()).isEqualTo(8);
        assertThat(createUserSchema.at("/properties/password/maxLength").asInt()).isEqualTo(16);
        assertThat(createUserSchema.at("/properties/role/enum").toString()).contains("Super Admin", "Records Officer");
        assertThat(document.at("/paths/~1api~1v1~1users/post/requestBody/content/application~1json/examples/Create user/value/email").asText())
                .isEqualTo("records.officer@example.test");
        assertThat(document.at("/paths/~1api~1v1~1users/post/requestBody/content/application~1json/schema/oneOf").isMissingNode())
                .isTrue();
        assertThat(document.at("/paths/~1api~1v1~1users/post/responses/201").isObject()).isTrue();
        assertThat(document.at("/paths/~1api~1v1~1users/post/responses/201/content/*~1*/schema/$ref").asText())
                .endsWith("/UserResponse");
        assertThat(document.at("/paths/~1api~1v1~1users/post/responses/400").toString())
                .contains("ApiError");
        assertThat(document.at("/paths/~1api~1v1~1users/post/responses/403").isObject()).isTrue();
        assertThat(document.at("/paths/~1api~1v1~1users/post/responses/409").isObject()).isTrue();

        JsonNode loginSchema = document.at("/components/schemas/AuthRequest");
        assertThat(loginSchema.at("/properties/username/description").asText()).contains("username", "email");
        assertThat(loginSchema.at("/properties/email").isMissingNode()).isTrue();
        assertThat(loginSchema.at("/properties/identifier").isMissingNode()).isTrue();
        assertThat(loginSchema.path("required").toString()).contains("username", "password");
        assertThat(document.at("/paths/~1api~1v1~1auth~1login/post/requestBody/content/application~1json/schema/oneOf").isMissingNode())
                .isTrue();
        assertThat(document.at("/paths/~1api~1v1~1auth~1login/post/responses/200").isObject()).isTrue();
        assertThat(document.at("/paths/~1api~1v1~1auth~1login/post/responses/200/content/*~1*/schema/$ref").asText())
                .endsWith("/AuthResponse");
        assertThat(document.at("/paths/~1api~1v1~1auth~1login/post/responses/401").isObject()).isTrue();
        assertThat(document.at("/paths/~1api~1v1~1auth~1login/post/responses/401/content/*~1*/schema/$ref").asText())
                .endsWith("/ApiError");
        assertThat(document.at("/paths/~1api~1v1~1auth~1login/post/responses/429").isObject()).isTrue();
    }
}
