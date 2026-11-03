package com.pms.api.visitor.service;

import com.pms.api.notification.service.NotificationService;
import com.pms.api.visitor.entity.Visit;
import com.pms.api.visitor.repository.VisitRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class VisitEndReminderScheduler {

    private static final long REMINDER_WINDOW_TOLERANCE_SECONDS = 10;

    private final VisitRepository visitRepository;
    private final NotificationService notificationService;

    public VisitEndReminderScheduler(VisitRepository visitRepository,
                                     NotificationService notificationService) {
        this.visitRepository = visitRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${app.visits.end-reminder.poll-interval:PT10S}")
    @Transactional
    public void sendRemindersNearFiveMinuteMark() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime windowStart = now.plusMinutes(5).minusSeconds(REMINDER_WINDOW_TOLERANCE_SECONDS);
        OffsetDateTime windowEnd = now.plusMinutes(5).plusSeconds(REMINDER_WINDOW_TOLERANCE_SECONDS);
        Instant sentAt = Instant.now();

        for (Visit visit : visitRepository.findVisitsDueForEndReminder(windowStart, windowEnd, now)) {
            int claimed = visitRepository.claimEndReminder(
                    visit.getId(), sentAt, windowStart, windowEnd, now);
            if (claimed == 1) {
                notificationService.notifyRoles(
                        "VISIT_ENDING_SOON",
                        "Visit session ending soon",
                        "A visit session is due to end in five minutes. Please ensure it concludes on time.",
                        visit.getPublicUuid(),
                        Set.of("Visitor Officer", "Duty Officer"));
            }
        }
    }
}
