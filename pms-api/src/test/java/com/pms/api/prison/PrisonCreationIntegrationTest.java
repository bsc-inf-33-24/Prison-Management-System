package com.pms.api.prison;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.api.security.jwt.JwtService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
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
@Import(com.pms.api.TestcontainersConfiguration.class)
class PrisonCreationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private String superAdminToken;

    @BeforeEach
    void setUp() {
        String username = "prison-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        User superAdmin = userRepository.saveAndFlush(
                new User("Prison Test Admin", username, "unused-password-hash", "Super Admin", Set.of()));
        superAdminToken = jwtService.generateToken(superAdmin);
    }

    @Test
    void createsPrisonWithoutClientCodeAndReturnsPersistedCells() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/prisons")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Prison Creation Test",
                                  "location": "Test District",
                                  "blocks": [
                                    {
                                      "name": "A",
                                      "cells": [
                                        { "cellNumber": 1, "capacity": 2 },
                                        { "cellNumber": 2, "capacity": 3 }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.blocks[0].cells.length()").value(2))
                .andExpect(jsonPath("$.blocks[0].cells[0].cellNumber").value(1))
                .andExpect(jsonPath("$.blocks[0].cells[1].cellNumber").value(2))
                .andReturn();

        String prisonId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("id").asText();

        mockMvc.perform(get("/api/v1/prisons/{prisonId}", prisonId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.blocks[0].cells.length()").value(2))
                .andExpect(jsonPath("$.blocks[0].cells[1].capacity").value(3));

        mockMvc.perform(get("/api/v1/prisons")
                        .queryParam("search", "Prison Creation Test")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(prisonId))
                .andExpect(jsonPath("$.content[0].code").doesNotExist());
    }
}
