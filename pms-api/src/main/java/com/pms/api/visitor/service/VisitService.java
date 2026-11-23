package com.pms.api.visitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.api.common.audit.AuditService;
import com.pms.api.health.entity.MedicalVisit;
import com.pms.api.health.repository.MedicalVisitRepository;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import com.pms.api.visitor.dto.VisitHistoryResponse;
import com.pms.api.visitor.dto.FamilyVisitHistoryResponse;
import com.pms.api.visitor.dto.FamilyVisitResponse;
import com.pms.api.visitor.dto.RecordFamilyVisitRequest;
import com.pms.api.visitor.dto.RecordLegalVisitRequest;
import com.pms.api.visitor.dto.VisitRequest;
import com.pms.api.visitor.dto.VisitResponse;
import com.pms.api.visitor.entity.FamilyVisitDetails;
import com.pms.api.visitor.entity.HealthVisitDetails;
import com.pms.api.visitor.entity.LegalVisitDetails;
import com.pms.api.visitor.entity.Visit;
import com.pms.api.visitor.entity.VisitType;
import com.pms.api.visitor.repository.VisitRepository;
import com.pms.api.security.PrisonAccessService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitService {

    private final VisitRepository visitRepository;
    private final MedicalVisitRepository medicalVisitRepository;
    private final InmateRepository inmateRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final ZoneId visitTimeZone;
    private final PrisonAccessService prisonAccess;

    public VisitService(VisitRepository visitRepository,
                        MedicalVisitRepository medicalVisitRepository,
                        InmateRepository inmateRepository,
                        UserRepository userRepository,
                        AuditService auditService,
                        ObjectMapper objectMapper,
                        @Value("${app.visits.time-zone:Africa/Blantyre}") String visitTimeZone,
                        PrisonAccessService prisonAccess) {
        this.visitRepository = visitRepository;
        this.medicalVisitRepository = medicalVisitRepository;
        this.inmateRepository = inmateRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.visitTimeZone = ZoneId.of(visitTimeZone);
        this.prisonAccess = prisonAccess;
    }

    @Transactional
    public VisitResponse recordVisit(VisitRequest request, String username) {
        if (request == null) {
            throw new IllegalArgumentException("Visit details are required.");
        }
        VisitType visitType = request.type();
        if (visitType == VisitType.FAMILY && isBlank(request.visitorFullName())) {
            throw new IllegalArgumentException("Visitor name is required.");
        }
        if (visitType == VisitType.HEALTH && (isBlank(request.sickness()) || isBlank(request.diagnosis()))) {
            throw new IllegalArgumentException("Health visit sickness and diagnosis are required.");
        }
        if (visitType == VisitType.LEGAL && isBlank(request.legalCaseNumber())) {
            throw new IllegalArgumentException("Legal case number is required.");
        }
        User user = userRepository.findByUsername(username).orElseThrow(UserNotFoundException::new);
        requireCreatePermission(user, visitType);
        Inmate inmate = inmateRepository.findByPublicUuid(request.inmateId())
                .orElseThrow(InmateNotFoundException::new);
        prisonAccess.requirePrison(inmate.getCurrentPrison()==null?null:inmate.getCurrentPrison().getPublicUuid());
        OffsetDateTime startDateTime = LocalDateTime.of(request.visitDate(), request.startTime())
                .atZone(visitTimeZone).toOffsetDateTime();
        OffsetDateTime endDateTime = LocalDateTime.of(request.visitDate(), request.endTime())
                .atZone(visitTimeZone).toOffsetDateTime();
        validateVisitTimes(startDateTime, endDateTime);

        UUID publicUuid = null;
        String complaint = null;
        String diagnosis = null;
        String medications = null;
        String recommendations = null;
        if (visitType == VisitType.HEALTH) {
            complaint = request.sickness().trim();
            diagnosis = request.diagnosis().trim();
            medications = normalizeOptionalText(request.medicationsProvided());
            recommendations = normalizeOptionalText(request.recommendations());
            MedicalVisit medicalVisit = medicalVisitRepository.save(new MedicalVisit(
                    inmate, user, request.visitDate(), complaint, diagnosis, medications, recommendations));
            publicUuid = medicalVisit.getPublicUuid();
        }

        Visit visit = new Visit(inmate, startDateTime, endDateTime,
                calculateDurationMinutes(startDateTime, endDateTime), user, visitType, publicUuid);
        if (visitType == VisitType.FAMILY) {
            visit.setFamilyDetails(new FamilyVisitDetails(
                    request.visitorFullName().trim(), normalizeOptionalText(request.itemsBrought())));
        } else if (visitType == VisitType.LEGAL) {
            visit.setLegalDetails(new LegalVisitDetails(
                    request.legalCaseNumber().trim(), normalizeOptionalText(request.legalRepresentative())));
        } else {
            visit.setHealthDetails(new HealthVisitDetails(complaint, diagnosis, medications, recommendations));
            visit.markReminderSent(java.time.Instant.now());
        }
        Visit saved = visitRepository.saveAndFlush(visit);
        audit(username, "VISIT_CREATED", "Visit", saved.getPublicUuid().toString(), "{}", visitSummary(saved));
        return VisitResponse.from(saved, canViewFamilyDetails(user), canViewLegalDetails(user),
                canViewHealthDetails(user));
    }

    @Transactional
    public FamilyVisitResponse recordFamilyVisit(RecordFamilyVisitRequest request, String username) {
        if (request == null) {
            throw new IllegalArgumentException("Family or friend visit details are required.");
        }
        VisitResponse response = recordVisit(new VisitRequest(VisitType.FAMILY, request.visitorFullName(), request.inmateId(),
                request.visitDate(), request.startTime(), request.endTime(), request.itemsBrought(),
                null, null, null, null, null, null), username);
        return FamilyVisitResponse.from(response);
    }

    @Transactional
    public VisitResponse recordLegalVisit(RecordLegalVisitRequest request, String username) {
        if (request == null) {
            throw new IllegalArgumentException("Legal visit details are required.");
        }
        return recordVisit(new VisitRequest(VisitType.LEGAL, null, request.inmateId(), request.visitDate(),
                request.startTime(), request.endTime(), null, null, null, null, null,
                request.legalCaseNumber(), request.legalRepresentative()), username);
    }

    @Transactional(readOnly = true)
    public Page<VisitResponse> searchVisits(UUID visitId, String visitorName, String visitType,
                                            UUID inmateId, String inmateName,
                                            OffsetDateTime visitDateFrom, OffsetDateTime visitDateTo,
                                            UUID loggedBy, int page, int size, String sort, String username) {
        validatePage(page, size);
        validateDateRange(visitDateFrom, visitDateTo);
        User user = userRepository.findByUsername(username).orElseThrow(UserNotFoundException::new);

        Specification<Visit> specification = Specification.unrestricted();
        UUID assignedPrison=prisonAccess.effectivePrison(null);
        if (assignedPrison!=null) specification=specification.and((root,query,cb)->
                cb.equal(root.get("inmate").get("currentPrison").get("publicUuid"),assignedPrison));
        if (visitId != null) {
            specification = specification.and(equalLong("id", visitRepository.findByPublicUuid(visitId)
                    .map(Visit::getId).orElse(-1L)));
        }
        if (!isBlank(visitorName)) {
            specification = specification.and(visitorNameContains(visitorName));
        }
        if (!isBlank(visitType)) {
            specification = specification.and(equalEnum("visitType", VisitType.valueOf(visitType.trim().toUpperCase(Locale.ROOT))));
        }
        specification = specification.and(allowedVisitTypes(user));
        if (inmateId != null) {
            specification = specification.and(equalRelatedId("inmate", inmateRepository.findByPublicUuid(inmateId)
                    .map(Inmate::getId).orElse(-1L)));
        }
        if (!isBlank(inmateName)) {
            specification = specification.and(inmateNameContains(inmateName));
        }
        if (visitDateFrom != null) {
            specification = specification.and(greaterThanOrEqualTo("startDateTime", visitDateFrom));
        }
        if (visitDateTo != null) {
            specification = specification.and(lessThanOrEqualTo("startDateTime", visitDateTo));
        }
        if (loggedBy != null) {
            specification = specification.and(equalRelatedId("loggedBy", userRepository.findByPublicUuid(loggedBy)
                    .map(User::getId).orElse(-1L)));
        }

        Pageable pageable = PageRequest.of(page, size, parseSort(sort, "startDateTime", Sort.Direction.DESC));
        return visitRepository.findAll(specification, pageable)
                .map(visit -> VisitResponse.from(visit, canViewFamilyDetails(user),
                        canViewLegalDetails(user), canViewHealthDetails(user)));
    }

    @Transactional(readOnly = true)
    public Page<VisitHistoryResponse> getInmateVisitHistory(UUID inmateId, String visitorName, String visitType,
                                                            OffsetDateTime visitDateFrom,
                                                            OffsetDateTime visitDateTo, int page, int size,
                                                            String sort, String username) {
        VisitType requestedType = isBlank(visitType) ? null
                : VisitType.valueOf(visitType.trim().toUpperCase(Locale.ROOT));
        return getInmateVisitHistory(inmateId, visitorName, requestedType, null, visitDateFrom,
                visitDateTo, page, size, sort, username);
    }

    @Transactional(readOnly = true)
    public Page<VisitHistoryResponse> getInmateVisitHistoryByType(UUID inmateId, VisitType visitType,
                                                                  String visitorName,
                                                                  OffsetDateTime visitDateFrom,
                                                                  OffsetDateTime visitDateTo, int page, int size,
                                                                  String sort, String username) {
        return getInmateVisitHistory(inmateId, visitorName, null, visitType, visitDateFrom,
                visitDateTo, page, size, sort, username);
    }

    @Transactional(readOnly = true)
    public Page<FamilyVisitHistoryResponse> getInmateFamilyVisitHistory(UUID inmateId, String visitorName,
                                                                        OffsetDateTime visitDateFrom,
                                                                        OffsetDateTime visitDateTo, int page,
                                                                        int size, String sort, String username) {
        return getInmateVisitHistory(inmateId, visitorName, null, VisitType.FAMILY, visitDateFrom,
                visitDateTo, page, size, sort, username).map(FamilyVisitHistoryResponse::from);
    }

    private Page<VisitHistoryResponse> getInmateVisitHistory(UUID inmateId, String visitorName,
                                                            VisitType requestedType, VisitType fixedType,
                                                            OffsetDateTime visitDateFrom,
                                                            OffsetDateTime visitDateTo, int page, int size,
                                                            String sort, String username) {
        Inmate inmate = inmateRepository.findByPublicUuid(inmateId).orElseThrow(InmateNotFoundException::new);
        User user = userRepository.findByUsername(username).orElseThrow(UserNotFoundException::new);
        prisonAccess.requirePrison(inmate.getCurrentPrison()==null?null:inmate.getCurrentPrison().getPublicUuid());
        validatePage(page, size);
        validateDateRange(visitDateFrom, visitDateTo);

        Specification<Visit> specification = Specification.where(
                (root, query, cb) -> cb.equal(root.get("inmate").get("id"), inmate.getId()));
        if (!isBlank(visitorName)) {
            specification = specification.and(visitorNameContains(visitorName));
        }
        VisitType effectiveType = fixedType == null ? requestedType : fixedType;
        if (effectiveType != null) {
            specification = specification.and(equalEnum("visitType", effectiveType));
        }
        specification = specification.and(allowedVisitTypes(user));
        if (visitDateFrom != null) {
            specification = specification.and(greaterThanOrEqualTo("startDateTime", visitDateFrom));
        }
        if (visitDateTo != null) {
            specification = specification.and(lessThanOrEqualTo("startDateTime", visitDateTo));
        }

        Pageable pageable = PageRequest.of(page, size, parseSort(sort, "startDateTime", Sort.Direction.DESC));
        return visitRepository.findAll(specification, pageable)
                .map(visit -> VisitHistoryResponse.from(visit, canViewFamilyDetails(user),
                        canViewLegalDetails(user)));
    }

    private static Specification<Visit> equalLong(String fieldName, Long value) {
        return (root, query, cb) -> cb.equal(root.get(fieldName), value);
    }

    private static Specification<Visit> allowedVisitTypes(User user) {
        if (hasRole(user, "Medical Officer")) {
            return equalEnum("visitType", VisitType.HEALTH);
        }
        if (hasRole(user, "Records Officer")) {
            return (root, query, cb) -> root.get("visitType").in(VisitType.FAMILY, VisitType.LEGAL);
        }
        if (hasRole(user, "Visitor Officer")) {
            return equalEnum("visitType", VisitType.FAMILY);
        }
        if (hasRole(user, "Case Officer")) {
            return equalEnum("visitType", VisitType.LEGAL);
        }
        if (hasRole(user, "Super Admin")) {
            return (root, query, cb) -> root.get("visitType").in(VisitType.FAMILY, VisitType.LEGAL);
        }
        throw new AccessDeniedException("Your role cannot access visits.");
    }

    private static <T> Specification<Visit> equalEnum(String fieldName, T value) {
        return (root, query, cb) -> cb.equal(root.get(fieldName), value);
    }

    private static Specification<Visit> equalRelatedId(String relation, Long id) {
        return (root, query, cb) -> cb.equal(root.get(relation).get("id"), id);
    }

    private static Specification<Visit> greaterThanOrEqualTo(String fieldName, OffsetDateTime threshold) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(fieldName), threshold);
    }

    private static Specification<Visit> lessThanOrEqualTo(String fieldName, OffsetDateTime threshold) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(fieldName), threshold);
    }

    private static Specification<Visit> visitorNameContains(String value) {
        String pattern = likePattern(value);
        return (root, query, cb) -> {
            Join<Object, Object> family = root.join("familyDetails", JoinType.LEFT);
            return cb.like(cb.lower(family.get("visitorFullName")), pattern, '!');
        };
    }

    private static Specification<Visit> inmateNumberContains(String value) {
        return (root, query, cb) -> {
            Join<Object, Object> inmate = root.join("inmate", JoinType.LEFT);
            String pattern = likePattern(value);
            return cb.like(cb.lower(inmate.get("inmateNumber").as(String.class)), pattern, '\\');
        };
    }

    private static Specification<Visit> inmateNameContains(String value) {
        return (root, query, cb) -> {
            Join<Object, Object> inmate = root.join("inmate", JoinType.LEFT);
            String pattern = likePattern(value);
            return cb.or(
                    cb.like(cb.lower(inmate.get("firstName")), pattern, '!'),
                    cb.like(cb.lower(inmate.get("lastName")), pattern, '!'));
        };
    }

    private static Specification<Visit> textContains(String field, String value) {
        String pattern = likePattern(value);
        return (root, query, cb) -> cb.like(cb.lower(root.get(field).as(String.class)), pattern, '!');
    }

    private static String likePattern(String value) {
        return "%" + escapeLikePattern(value.trim().toLowerCase(Locale.ROOT)) + "%";
    }

    private static String escapeLikePattern(String value) {
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    private static Sort parseSort(String sort, String defaultField, Sort.Direction defaultDirection) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(defaultDirection, defaultField);
        }

        String[] parts = sort.split(",");
        String field = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim() : defaultDirection.name();
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;

        String normalizedField = switch (field) {
            case "visitId" -> "id";
            case "inmateId" -> "inmate.id";
            case "visitDate", "visitDateTime", "startDateTime" -> "startDateTime";
            case "endDateTime" -> "endDateTime";
            case "loggedBy" -> "loggedBy.id";
            case "visitorName" -> "familyDetails.visitorFullName";
            case "inmateName" -> "inmate.firstName";
            default -> defaultField;
        };
        return Sort.by(dir, normalizedField);
    }

    private static void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative.");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100.");
        }
    }

    private static void validateDateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("visitDateFrom must be before or equal to visitDateTo.");
        }
    }

    private static void validateVisitTimes(OffsetDateTime startDateTime, OffsetDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            throw new IllegalArgumentException("startDateTime and endDateTime are required.");
        }
        if (endDateTime.isBefore(startDateTime)) {
            throw new IllegalArgumentException("endDateTime must not be earlier than startDateTime.");
        }
    }

    private static int calculateDurationMinutes(OffsetDateTime startDateTime, OffsetDateTime endDateTime) {
        try {
            return Math.toIntExact(Duration.between(startDateTime, endDateTime).toMinutes());
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Visit duration is too large.", exception);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizeOptionalText(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static void requireCreatePermission(User user, VisitType visitType) {
        boolean allowed = switch (visitType) {
            case FAMILY -> hasRole(user, "Visitor Officer", "Records Officer", "Super Admin");
            case LEGAL -> hasRole(user, "Case Officer", "Records Officer", "Super Admin");
            case HEALTH -> hasRole(user, "Medical Officer", "Records Officer", "Super Admin");
        };
        if (!allowed) {
            throw new AccessDeniedException("Your role cannot record this visit type.");
        }
    }

    private static boolean canViewFamilyDetails(User user) {
        return hasRole(user, "Visitor Officer", "Records Officer", "Super Admin");
    }

    private static boolean canViewLegalDetails(User user) {
        return hasRole(user, "Case Officer", "Records Officer", "Super Admin");
    }

    private static boolean canViewHealthDetails(User user) {
        return hasRole(user, "Medical Officer", "Super Admin");
    }

    private static boolean hasRole(User user, String... roles) {
        for (String role : roles) {
            if (role.equals(user.getRole())) {
                return true;
            }
        }
        return false;
    }

    private void audit(String username, String action, String entityName, String publicRecordId,
                       String oldValues, String newValues) {
        auditService.record(username, action, entityName, publicRecordId,
                oldValues == null ? "{}" : oldValues, newValues == null ? "{}" : newValues);
    }

    private String visitSummary(Visit visit) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "visitId", visit.getPublicUuid(),
                    "visitType", visit.getVisitType()));
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    public static class InmateNotFoundException extends RuntimeException {
    }

    public static class UserNotFoundException extends RuntimeException {
    }
}
