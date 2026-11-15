package com.pms.api.health.service;

import com.pms.api.health.dto.MedicalHistoryResponse;
import com.pms.api.health.dto.MedicalVisitResponse;
import com.pms.api.health.dto.RecordMedicalVisitRequest;
import com.pms.api.health.entity.MedicalVisit;
import com.pms.api.health.repository.MedicalVisitRepository;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.notification.service.NotificationService;
import com.pms.api.security.PrisonAccessService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import com.pms.api.visitor.entity.Visit;
import com.pms.api.visitor.entity.HealthVisitDetails;
import com.pms.api.visitor.entity.VisitType;
import com.pms.api.visitor.repository.VisitRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalVisitService {

    private final MedicalVisitRepository medicalVisitRepository;
    private final VisitRepository visitRepository;
    private final InmateRepository inmateRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PrisonAccessService prisonAccess;
    private final ZoneId visitTimeZone;

    public MedicalVisitService(MedicalVisitRepository medicalVisitRepository,
                               VisitRepository visitRepository,
                               InmateRepository inmateRepository,
                               UserRepository userRepository,
                               NotificationService notificationService,
                               PrisonAccessService prisonAccess,
                               @Value("${app.visits.time-zone:Africa/Blantyre}") String visitTimeZone) {
        this.medicalVisitRepository = medicalVisitRepository;
        this.visitRepository = visitRepository;
        this.inmateRepository = inmateRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.prisonAccess = prisonAccess;
        this.visitTimeZone = ZoneId.of(visitTimeZone);
    }

    @Transactional
    public MedicalVisitResponse recordVisit(RecordMedicalVisitRequest request, String username) {
        Inmate inmate = inmateRepository.findByPublicUuid(request.inmateId())
                .orElseThrow(InmateNotFoundException::new);
        prisonAccess.requirePrison(inmate.getCurrentPrison()==null?null:inmate.getCurrentPrison().getPublicUuid());
        User medicalOfficer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));

        MedicalVisit visit = new MedicalVisit(
                inmate,
                medicalOfficer,
                request.visitDate(),
                request.sickness().trim(),
                request.diagnosis().trim(),
                normalizeOptionalText(request.medicationsProvided()),
                normalizeOptionalText(request.recommendations()));
        MedicalVisit saved = medicalVisitRepository.save(visit);
        OffsetDateTime startDateTime = LocalDateTime.of(request.visitDate(), java.time.LocalTime.NOON)
                .atZone(visitTimeZone).toOffsetDateTime();
        OffsetDateTime endDateTime = startDateTime.plusMinutes(15);
        Visit sharedVisit = new Visit(
                inmate,
                startDateTime,
                endDateTime,
                15,
                medicalOfficer,
                VisitType.HEALTH,
                saved.getPublicUuid());
        sharedVisit.setHealthDetails(new HealthVisitDetails(
                request.sickness().trim(),
                request.diagnosis().trim(),
                normalizeOptionalText(request.medicationsProvided()),
                normalizeOptionalText(request.recommendations())));
        sharedVisit.markReminderSent(java.time.Instant.now());
        visitRepository.save(sharedVisit);
        notificationService.notifyRoles(
                "HEALTH_VISIT_RECORDED",
                "Health visit recorded",
                "A health visit has been recorded for an inmate.",
                saved.getPublicUuid(),
                Set.of("Medical Officer", "Super Admin"));
        return MedicalVisitResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public MedicalHistoryResponse getHistory(UUID inmateId, String name) {
        Inmate inmate = inmateRepository.findByPublicUuid(inmateId)
                .orElseThrow(InmateNotFoundException::new);
        prisonAccess.requirePrison(inmate.getCurrentPrison()==null?null:inmate.getCurrentPrison().getPublicUuid());
        List<MedicalVisitResponse> visits = medicalVisitRepository
                .findByInmateIdAndInmateNameOrderByVisitDateDescIdDesc(
                        inmate.getId(), normalizeOptionalText(name))
                .stream()
                .map(MedicalVisitResponse::from)
                .toList();
        return MedicalHistoryResponse.from(inmate, visits);
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static class InmateNotFoundException extends RuntimeException {
    }
}
