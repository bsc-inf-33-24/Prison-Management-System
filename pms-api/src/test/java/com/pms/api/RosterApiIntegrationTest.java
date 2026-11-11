package com.pms.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pms.api.notification.entity.Notification;
import com.pms.api.notification.repository.NotificationRepository;
import com.pms.api.prison.entity.Prison;
import com.pms.api.prison.repository.PrisonRepository;
import com.pms.api.security.jwt.JwtService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.time.LocalDate;
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
@Import(TestcontainersConfiguration.class)
class RosterApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PrisonRepository prisonRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private Prison prison;
    private Prison otherPrison;
    private User dutyOfficer;
    private User assignedOfficer;
    private User otherOfficer;
    private User superAdmin;
    private String dutyOfficerToken;
    private String assignedOfficerToken;
    private String otherOfficerToken;
    private String superAdminToken;

    @BeforeEach
    void setUp() {
        prison = prisonRepository.saveAndFlush(new Prison(
                "R" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                "Roster Test Prison", null));
        otherPrison = prisonRepository.saveAndFlush(new Prison(
                "R" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                "Other Roster Test Prison", null));

        dutyOfficer = saveUser("Duty Officer", prison);
        assignedOfficer = saveUser("Prison Officer", prison);
        otherOfficer = saveUser("Prison Officer", otherPrison);
        superAdmin = saveUser("Super Admin", null);

        dutyOfficerToken = jwtService.generateToken(dutyOfficer);
        assignedOfficerToken = jwtService.generateToken(assignedOfficer);
        otherOfficerToken = jwtService.generateToken(otherOfficer);
        superAdminToken = jwtService.generateToken(superAdmin);
    }

    @Test
    void dutyOfficerCreatesDraftAndCannotAssignItToAnotherPrison() throws Exception {
        UUID rosterId = createRoster(dutyOfficerToken, null,
                LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 13));

        mockMvc.perform(get("/api/v1/rosters/{rosterId}", rosterId)
                        .header("Authorization", bearer(dutyOfficerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.shifts").isEmpty());

        mockMvc.perform(post("/api/v1/rosters")
                        .header("Authorization", bearer(dutyOfficerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rosterRequest(otherPrison.getPublicUuid(),
                                LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 13))))
                .andExpect(status().isForbidden());

        UUID adminRosterId = createRoster(superAdminToken, otherPrison.getPublicUuid(),
                LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 13));
        mockMvc.perform(get("/api/v1/rosters/{rosterId}", adminRosterId)
                        .header("Authorization", bearer(dutyOfficerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsIneligibleOfficersAndOverlappingAssignments() throws Exception {
        UUID rosterId = createRoster(dutyOfficerToken, null,
                LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 12));

        mockMvc.perform(post("/api/v1/rosters/{rosterId}/shifts", rosterId)
                        .header("Authorization", bearer(dutyOfficerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shiftRequest(otherOfficer.getPublicUuid(),
                                "2026-11-12T08:00:00+02:00", "2026-11-12T12:00:00+02:00", "North post")))
                .andExpect(status().isBadRequest());

        addShift(rosterId, assignedOfficer.getPublicUuid(),
                "2026-11-12T08:00:00+02:00", "2026-11-12T12:00:00+02:00", "North post");
        mockMvc.perform(post("/api/v1/rosters/{rosterId}/shifts", rosterId)
                        .header("Authorization", bearer(dutyOfficerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shiftRequest(assignedOfficer.getPublicUuid(),
                                "2026-11-12T11:00:00+02:00", "2026-11-12T13:00:00+02:00", "South post")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The officer already has an overlapping shift."));
    }

    @Test
    void publishesNotifiesOncePerOfficerAndScopesPublishedShiftsToTheAuthenticatedUser() throws Exception {
        UUID rosterId = createRoster(dutyOfficerToken, null,
                LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 12));
        UUID firstShift = addShift(rosterId, assignedOfficer.getPublicUuid(),
                "2026-11-12T08:00:00+02:00", "2026-11-12T12:00:00+02:00", "North post");
        UUID secondShift = addShift(rosterId, assignedOfficer.getPublicUuid(),
                "2026-11-12T13:00:00+02:00", "2026-11-12T17:00:00+02:00", "South post");

        mockMvc.perform(get("/api/v1/me/shifts")
                        .param("from", "2026-11-12")
                        .param("to", "2026-11-12")
                        .header("Authorization", bearer(assignedOfficerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/api/v1/rosters/{rosterId}/publish", rosterId)
                        .header("Authorization", bearer(dutyOfficerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        assertThat(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(assignedOfficer.getId()))
                .extracting(Notification::getType)
                .containsExactly("DUTY_ROSTER_PUBLISHED");
        Notification publication = notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(assignedOfficer.getId()).getFirst();
        assertThat(publication.getRelatedRecordId()).isEqualTo(rosterId.toString());
        assertThat(publication.getMessage())
                .contains(rosterId.toString(), firstShift.toString(), secondShift.toString(),
                        "North post", "South post");

        mockMvc.perform(get("/api/v1/me/shifts")
                        .param("from", "2026-11-12")
                        .param("to", "2026-11-12")
                        .param("userId", otherOfficer.getPublicUuid().toString())
                        .header("Authorization", bearer(assignedOfficerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rosterId").value(rosterId.toString()));

        mockMvc.perform(get("/api/v1/me/shifts")
                        .param("from", "2026-11-12")
                        .param("to", "2026-11-12")
                        .header("Authorization", bearer(otherOfficerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void changesAndCancellationsNotifyTheAffectedOfficer() throws Exception {
        UUID rosterId = createRoster(dutyOfficerToken, null,
                LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 12));
        UUID shiftId = addShift(rosterId, assignedOfficer.getPublicUuid(),
                "2026-11-12T08:00:00+02:00", "2026-11-12T12:00:00+02:00", "North post");
        mockMvc.perform(post("/api/v1/rosters/{rosterId}/publish", rosterId)
                        .header("Authorization", bearer(dutyOfficerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/rosters/{rosterId}/shifts/{shiftId}", rosterId, shiftId)
                        .header("Authorization", bearer(dutyOfficerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shiftRequest(assignedOfficer.getPublicUuid(),
                                "2026-11-12T09:00:00+02:00", "2026-11-12T13:00:00+02:00", "North post")))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/rosters/{rosterId}/shifts/{shiftId}", rosterId, shiftId)
                        .header("Authorization", bearer(dutyOfficerToken)))
                .andExpect(status().isNoContent());

        assertThat(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(assignedOfficer.getId()))
                .extracting(Notification::getType)
                .containsExactly("DUTY_SHIFT_CANCELLED", "DUTY_SHIFT_UPDATED", "DUTY_ROSTER_PUBLISHED");
        assertThat(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(otherOfficer.getId()))
                .isEmpty();
    }

    private User saveUser(String role, Prison assignedPrison) {
        String username = "rt" + UUID.randomUUID().toString().replace("-", "").substring(0, 13);
        User user = new User(role, username, "unused-password-hash", role, Set.of());
        user.assignPrison(assignedPrison);
        return userRepository.saveAndFlush(user);
    }

    private UUID createRoster(String token, UUID prisonId, LocalDate from, LocalDate to) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/rosters")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rosterRequest(prisonId, from, to)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.path("id").asText());
    }

    private UUID addShift(UUID rosterId, UUID officerId, String startAt, String endAt,
                          String location) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/rosters/{rosterId}/shifts", rosterId)
                        .header("Authorization", bearer(dutyOfficerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shiftRequest(officerId, startAt, endAt, location)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText());
    }

    private String rosterRequest(UUID prisonId, LocalDate from, LocalDate to) {
        ObjectNode request = objectMapper.createObjectNode();
        if (prisonId != null) {
            request.put("prisonId", prisonId.toString());
        }
        request.put("periodStart", from.toString());
        request.put("periodEnd", to.toString());
        return request.toString();
    }

    private String shiftRequest(UUID officerId, String startAt, String endAt, String location) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("officerId", officerId.toString());
        request.put("startAt", startAt);
        request.put("endAt", endAt);
        request.put("location", location);
        return request.toString();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
