package com.pms.api.inmate.service;

import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.notification.service.NotificationService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProjectedReleaseReminderScheduler {
    private final InmateRepository inmateRepository;
    private final NotificationService notificationService;

    public ProjectedReleaseReminderScheduler(InmateRepository inmateRepository,
                                             NotificationService notificationService) {
        this.inmateRepository = inmateRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${app.inmates.release-reminder.cron:0 0 8 * * *}", zone = "${app.visits.time-zone:Africa/Blantyre}")
    @Transactional
    public void notifyApproachingReleases() {
        LocalDate reminderDate = LocalDate.now().plusDays(30);
        for (Inmate inmate : inmateRepository.findDueForReleaseReminder(reminderDate)) {
            if (inmateRepository.claimReleaseReminder(inmate.getId(), Instant.now()) == 1) {
                notificationService.notifyRoles(
                        "INMATE_RELEASE_APPROACHING",
                        "Projected inmate release approaching",
                        inmate.getFirstName() + " " + inmate.getLastName()
                                + " has a projected release date 30 days from today ("
                                + inmate.getProjectedReleaseDate() + "). Please review release arrangements.",
                        inmate.getPublicUuid(),
                        Set.of("Records Officer", "Duty Officer"));
            }
        }
    }
}
