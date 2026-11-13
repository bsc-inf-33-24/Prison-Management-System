package com.pms.api.inmate.service;

import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.CourtDateReminderRepository;
import com.pms.api.notification.service.NotificationService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CourtDateReminderScheduler {
    private static final Set<Integer> REMINDER_DAYS = Set.of(30, 7, 1);
    private final CourtDateReminderRepository reminders;
    private final NotificationService notifications;
    private final ObjectProvider<CourtDateEmailDelivery> emailDelivery;
    public CourtDateReminderScheduler(CourtDateReminderRepository reminders, NotificationService notifications,
            ObjectProvider<CourtDateEmailDelivery> emailDelivery) {
        this.reminders = reminders; this.notifications = notifications; this.emailDelivery = emailDelivery;
    }
    @Scheduled(cron = "${app.inmates.court-reminder.cron:0 0 8 * * *}", zone = "${app.visits.time-zone:Africa/Blantyre}")
    @Transactional
    public void notifyUpcomingHearings() {
        LocalDate today = LocalDate.now();
        CourtDateEmailDelivery mailer = emailDelivery.getIfAvailable();
        for (int days : REMINDER_DAYS) {
            LocalDate courtDate = today.plusDays(days);
            for (Inmate inmate : reminders.findRemandHearings(courtDate)) {
                String message = inmate.getFirstName() + " " + inmate.getLastName()
                        + " has a court date on " + courtDate + " (" + days + " days from today).";
                if (reminders.claim(inmate.getId(), courtDate, days, "IN_APP", Instant.now()) == 1) {
                    notifications.notifyRoles("REMAND_COURT_DATE_APPROACHING", "Upcoming remand court date",
                            message, inmate.getPublicUuid(), Set.of("Records Officer", "Duty Officer"));
                }
                if (mailer != null && reminders.claim(inmate.getId(), courtDate, days, "EMAIL", Instant.now()) == 1) {
                    if (!mailer.send(inmate, days)) {
                        reminders.release(inmate.getId(), courtDate, days, "EMAIL");
                    }
                }
            }
        }
    }
}
