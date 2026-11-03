package com.pms.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pms.api.health.dto.RecordMedicalVisitRequest;
import com.pms.api.health.service.MedicalVisitService;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.notification.entity.Notification;
import com.pms.api.notification.repository.NotificationRepository;
import com.pms.api.security.jwt.JwtService;
import com.pms.api.user.dto.CreateUserRequest;
import com.pms.api.user.dto.UpdateUserDetailsRequest;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import com.pms.api.user.service.UserService;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class NotificationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MedicalVisitService medicalVisitService;

    @Autowired
    private InmateRepository inmateRepository;

    @Autowired
    private JwtService jwtService;

    private User superAdmin;
    private User firstUser;
    private User secondUser;
    private String firstUserToken;
    private String secondUserToken;

    @BeforeEach
    void setUp() {
        superAdmin = saveUser("Super Admin");
        firstUser = saveUser("Records Officer");
        secondUser = saveUser("Visitor Officer");
        firstUserToken = jwtService.generateToken(firstUser);
        secondUserToken = jwtService.generateToken(secondUser);
    }

    @Test
    void notificationFeedIsScopedToCurrentUserAndSupportsReadOperations() throws Exception {
        Notification firstNotification = notificationRepository.save(new Notification(
                firstUser.getId(), "INMATE_REGISTERED", "Inmate registered",
                "A new inmate has been registered.", UUID.randomUUID().toString()));
        notificationRepository.save(new Notification(
                secondUser.getId(), "INCIDENT_REPORTED", "Incident reported",
                "A new incident has been reported.", UUID.randomUUID().toString()));

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bearer(firstUserToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Inmate registered"))
                .andExpect(jsonPath("$.content[0].isRead").value(false));

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", firstNotification.getId())
                        .header("Authorization", bearer(secondUserToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", firstNotification.getId())
                        .header("Authorization", bearer(firstUserToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", bearer(firstUserToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void creatingAnAccountNotifiesTheNewUserAndActiveSuperAdmins() {
        String username = uniqueUsername();
        userService.create(new CreateUserRequest(
                "New Records Officer", username, null, "GoodPass1!", "Records Officer"));
        User createdUser = userRepository.findByUsername(username).orElseThrow();

        assertThat(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(createdUser.getId()))
                .extracting(Notification::getType)
                .containsExactly("USER_ACCOUNT_CREATED");
        assertThat(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(superAdmin.getId()))
                .extracting(Notification::getRelatedRecordId)
                .contains(createdUser.getPublicUuid().toString());
    }

    @Test
    void accountStatusAndProfileChangesNotifyOnlyWhenTheAccountActuallyChanges() {
        userService.updateDetails(firstUser.getPublicUuid(), new UpdateUserDetailsRequest(
                firstUser.getFullName(), null, null, null, null));
        assertThat(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(firstUser.getId())).isEmpty();

        userService.deactivate(firstUser.getPublicUuid());
        assertThat(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(firstUser.getId())).isEmpty();
        assertThat(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(superAdmin.getId()))
                .extracting(Notification::getType)
                .contains("USER_ACCOUNT_DEACTIVATED");

        userService.reactivate(firstUser.getPublicUuid());
        userService.updateDetails(firstUser.getPublicUuid(), new UpdateUserDetailsRequest(
                "Updated " + firstUser.getFullName(), null, null, null, null));

        assertThat(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(firstUser.getId()))
                .extracting(Notification::getType)
                .containsExactlyInAnyOrder("USER_ACCOUNT_REACTIVATED", "USER_ACCOUNT_UPDATED");
    }

    @Test
    void readAllOnlyChangesTheAuthenticatedUsersNotifications() throws Exception {
        notificationRepository.save(new Notification(
                firstUser.getId(), "INMATE_REGISTERED", "Inmate registered", "A new inmate was registered.", null));
        notificationRepository.save(new Notification(
                firstUser.getId(), "INCIDENT_REPORTED", "Incident reported", "A new incident was reported.", null));
        notificationRepository.save(new Notification(
                secondUser.getId(), "INCIDENT_REPORTED", "Incident reported", "A new incident was reported.", null));

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", bearer(firstUserToken)))
                .andExpect(status().isNoContent());

        assertThat(notificationRepository.countByRecipientUserIdAndIsRead(firstUser.getId(), false)).isZero();
        assertThat(notificationRepository.countByRecipientUserIdAndIsRead(secondUser.getId(), false)).isEqualTo(1);
    }

    @Test
    void recordingAHealthVisitNotifiesMedicalOfficersAndSuperAdmins() {
        User medicalOfficer = saveUser("Medical Officer");
        Inmate inmate = inmateRepository.saveAndFlush(new Inmate(
                "Health", "Test", LocalDate.of(1990, 1, 1), "Other",
                LocalDate.now(), null, LocalDate.now(), LocalDate.now().plusDays(30)));

        medicalVisitService.recordVisit(new RecordMedicalVisitRequest(
                inmate.getPublicUuid(), LocalDate.now(), "Confidential complaint",
                "Confidential diagnosis", null, null), medicalOfficer.getUsername());

        assertThat(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(medicalOfficer.getId()))
                .extracting(Notification::getType)
                .containsExactly("HEALTH_VISIT_RECORDED");
        assertThat(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(superAdmin.getId()))
                .extracting(Notification::getType)
                .contains("HEALTH_VISIT_RECORDED");
        assertThat(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(medicalOfficer.getId())
                .getFirst().getMessage())
                .doesNotContain("Confidential");
    }

    private User saveUser(String role) {
        String username = uniqueUsername();
        return userRepository.saveAndFlush(new User(
                role, username, "unused-password-hash", role, Set.of()));
    }

    private static String uniqueUsername() {
        return "nt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
