package com.pms.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ApiVersioningAndSwaggerTest {

    @Autowired
    private MockMvc mockMvc;

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
    }
}
