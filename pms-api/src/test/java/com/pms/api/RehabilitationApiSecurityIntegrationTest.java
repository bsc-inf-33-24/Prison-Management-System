package com.pms.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RehabilitationApiSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void onlyProgramFacilitatorsAndAdminsCanCreatePrograms() throws Exception {
        String programRequest = """
                {"name":"Role Access Test Program","description":"Security test",
                 "instructor":"Test Instructor","schedule":"Monday"}
                """;

        mockMvc.perform(post("/api/v1/programs")
                        .header("Authorization", "Bearer " + tokenForRole("Program Facilitator"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(programRequest))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/programs")
                        .header("Authorization", "Bearer " + tokenForRole("Prison Officer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(programRequest))
                .andExpect(status().isForbidden());
    }

    private String tokenForRole(String role) {
        String username = "s" + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
        User user = userRepository.saveAndFlush(
                new User("Security Test", username, "unused-password-hash", role, Set.of()));
        return jwtService.generateToken(user);
    }
}