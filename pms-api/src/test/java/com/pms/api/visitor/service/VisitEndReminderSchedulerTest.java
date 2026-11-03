package com.pms.api.visitor.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pms.api.notification.service.NotificationService;
import com.pms.api.visitor.entity.Visit;
import com.pms.api.visitor.repository.VisitRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VisitEndReminderSchedulerTest {

    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final VisitEndReminderScheduler scheduler =
            new VisitEndReminderScheduler(visitRepository, notificationService);

    @Test
    void notifiesOnceWhenVisitReminderIsClaimed() {
        Visit visit = mock(Visit.class);
        UUID visitUuid = UUID.randomUUID();
        when(visit.getId()).thenReturn(1L);
        when(visit.getPublicUuid()).thenReturn(visitUuid);
        when(visitRepository.findVisitsDueForEndReminder(any(), any(), any())).thenReturn(List.of(visit));
        when(visitRepository.claimEndReminder(anyLong(), any(Instant.class),
                any(OffsetDateTime.class), any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(1);

        scheduler.sendRemindersNearFiveMinuteMark();

        verify(notificationService).notifyRoles(
                "VISIT_ENDING_SOON",
                "Visit session ending soon",
                "A visit session is due to end in five minutes. Please ensure it concludes on time.",
                visitUuid,
                java.util.Set.of("Visitor Officer", "Duty Officer"));
    }

    @Test
    void doesNotNotifyWhenAnotherSchedulerInstanceAlreadyClaimedTheReminder() {
        Visit visit = mock(Visit.class);
        when(visit.getId()).thenReturn(1L);
        when(visitRepository.findVisitsDueForEndReminder(any(), any(), any())).thenReturn(List.of(visit));
        when(visitRepository.claimEndReminder(anyLong(), any(Instant.class),
                any(OffsetDateTime.class), any(OffsetDateTime.class), any(OffsetDateTime.class))).thenReturn(0);

        scheduler.sendRemindersNearFiveMinuteMark();

        verify(notificationService, never()).notifyRoles(
                any(), any(), any(), any(), any());
    }
}
