package com.pms.api.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pms.api.auth.dto.AuthRequest;
import com.pms.api.common.error.ApiExceptionHandler;
import jakarta.validation.Valid;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class AuthRequestValidationTest {

    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ValidationProbeController())
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void returnsStandardApiErrorForInvalidCredentials() throws Exception {
        mockMvc.perform(post("/validation-probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("One or more request fields are invalid."));
    }

    @Test
    void doesNotEchoSubmittedPasswordInValidationResponse() throws Exception {
        String submittedPassword = "not-for-response";
        String response = mockMvc.perform(post("/validation-probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"\",\"password\":\"" + submittedPassword + "\"}"))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertFalse(response.contains(submittedPassword));
        assertFalse(response.contains("\"errors\""));
    }

    @Test
    void rejectsMultipleLoginIdentifiers() throws Exception {
        mockMvc.perform(post("/validation-probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"records01\",\"email\":\"records@example.test\",\"password\":\"Records123!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptsUnifiedIdentifierAndExistingUsernameOrEmailPayloads() throws Exception {
        List<String> requests = List.of(
                "{\"identifier\":\"records01\",\"password\":\"Records123!\"}",
                "{\"username\":\"records01\",\"password\":\"Records123!\"}",
                "{\"email\":\"records@example.test\",\"password\":\"Records123!\"}");

        for (String request : requests) {
            mockMvc.perform(post("/validation-probe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk());
        }
    }

    @RestController
    static class ValidationProbeController {

        @PostMapping("/validation-probe")
        void validate(@Valid @RequestBody AuthRequest request) {
        }
    }
}
