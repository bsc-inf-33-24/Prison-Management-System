package com.pms.api.visitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.api.common.audit.AuditService;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import com.pms.api.visitor.dto.UpdateVisitRequest;
import com.pms.api.visitor.dto.VisitHistoryResponse;
import com.pms.api.visitor.dto.VisitRequest;
import com.pms.api.visitor.dto.VisitResponse;
import com.pms.api.visitor.entity.Visit;
import com.pms.api.visitor.repository.VisitRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
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
    private final InmateRepository inmateRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final ZoneId visitTimeZone;

    public VisitService(VisitRepository visitRepository,
                        InmateRepository inmateRepository,
                        UserRepository userRepository,
                        AuditService auditService,
                        ObjectMapper objectMapper,
                        @Value("${app.visits.time-zone:Africa/Blantyre}") String visitTimeZone) {
        this.visitRepository = visitRepository;
        this.inmateRepository = inmateRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.visitTimeZone = ZoneId.of(visitTimeZone);
    }

    @Transactional
    public VisitResponse recordVisit(VisitRequest request, String username) {
        if (request == null) {
            throw new IllegalArgumentException("Visit details are required.");
        }
        if (isBlank(request.visitorFullName())) {
            throw new IllegalArgumentException("Visitor name is required.");
        }
        Inmate inmate = inmateRepository.findByPublicUuid(request.inmateId())
                .orElseThrow(InmateNotFoundException::new);
        OffsetDateTime startDateTime = LocalDateTime.of(request.visitDate(), request.startTime())
                .atZone(visitTimeZone).toOffsetDateTime();
        OffsetDateTime endDateTime = LocalDateTime.of(request.visitDate(), request.endTime())
                .atZone(visitTimeZone).toOffsetDateTime();
        validateVisitTimes(startDateTime, endDateTime);

        User user = userRepository.findByUsername(username).orElseThrow(UserNotFoundException::new);
        Visit visit = new Visit(inmate, startDateTime, endDateTime,
                calculateDurationMinutes(startDateTime, endDateTime), user,
                request.visitorFullName().trim());
        Visit saved = visitRepository.saveAndFlush(visit);
        audit(username, "VISIT_CREATED", "Visit", saved.getPublicUuid().toString(), "{}", visitSummary(saved));
        return VisitResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public VisitResponse getVisit(UUID visitId) {
        return VisitResponse.from(findVisit(visitId));
    }

    @Transactional(readOnly = true)
    public Page<VisitResponse> searchVisits(UUID visitId, String visitorName, UUID inmateId,
                                            String inmateName,
                                            OffsetDateTime visitDateFrom, OffsetDateTime visitDateTo,
                                            UUID loggedBy, int page, int size, String sort) {
        validatePage(page, size);
        validateDateRange(visitDateFrom, visitDateTo);

        Specification<Visit> specification = Specification.where((Specification<Visit>) null);
        if (visitId != null) {
            specification = specification.and(equalLong("id", visitRepository.findByPublicUuid(visitId)
                    .map(Visit::getId).orElse(-1L)));
        }
        if (!isBlank(visitorName)) {
            specification = specification.and(visitorNameContains(visitorName));
        }
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
        return visitRepository.findAll(specification, pageable).map(VisitResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<VisitHistoryResponse> getInmateVisitHistory(UUID inmateId, String visitorName,
                                                            OffsetDateTime visitDateFrom,
                                                            OffsetDateTime visitDateTo, int page, int size,
                                                            String sort) {
        Inmate inmate = inmateRepository.findByPublicUuid(inmateId).orElseThrow(InmateNotFoundException::new);
        validatePage(page, size);
        validateDateRange(visitDateFrom, visitDateTo);

        Specification<Visit> specification = Specification.where(
                (root, query, cb) -> cb.equal(root.get("inmate").get("id"), inmate.getId()));
        if (!isBlank(visitorName)) {
            specification = specification.and(visitorNameContains(visitorName));
        }
        if (visitDateFrom != null) {
            specification = specification.and(greaterThanOrEqualTo("startDateTime", visitDateFrom));
        }
        if (visitDateTo != null) {
            specification = specification.and(lessThanOrEqualTo("startDateTime", visitDateTo));
        }

        Pageable pageable = PageRequest.of(page, size, parseSort(sort, "startDateTime", Sort.Direction.DESC));
        return visitRepository.findAll(specification, pageable).map(VisitHistoryResponse::from);
    }

    @Transactional
    public VisitResponse updateVisit(UUID visitId, UpdateVisitRequest request, String username) {
        Visit visit = findVisit(visitId);
        if (request == null) {
            throw new IllegalArgumentException("Visit details are required.");
        }
        validateVisitTimes(request.startDateTime(), request.endDateTime());

        Visit prior = new Visit(visit.getInmate(), visit.getStartDateTime(), visit.getEndDateTime(),
                visit.getDurationMinutes(),
                visit.getLoggedBy(), visit.getVisitorFullName());
        visit.updateDetails(request.startDateTime(), request.endDateTime(),
                calculateDurationMinutes(request.startDateTime(), request.endDateTime()));
        Visit saved = visitRepository.saveAndFlush(visit);
        audit(username, "VISIT_UPDATED", "Visit", saved.getPublicUuid().toString(),
                visitSummary(prior), visitSummary(saved));
        return VisitResponse.from(saved);
    }

    public Visit findVisit(UUID visitId) {
        return visitRepository.findByPublicUuid(visitId).orElseThrow(VisitNotFoundException::new);
    }

    private static Specification<Visit> equalLong(String fieldName, Long value) {
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
        return textContains("visitorFullName", value);
    }

    private static Specification<Visit> inmateNameContains(String value) {
        return (root, query, cb) -> {
            Join<Object, Object> inmate = root.join("inmate", JoinType.LEFT);
            String pattern = likePattern(value);
            return cb.or(
                    cb.like(cb.lower(inmate.get("firstName").as(String.class)), pattern, '\\'),
                    cb.like(cb.lower(inmate.get("lastName").as(String.class)), pattern, '\\'));
        };
    }

    private static Specification<Visit> textContains(String field, String value) {
        String pattern = likePattern(value);
        return (root, query, cb) -> cb.like(cb.lower(root.get(field).as(String.class)), pattern, '\\');
    }

    private static String likePattern(String value) {
        return "%" + escapeLikePattern(value.trim().toLowerCase(Locale.ROOT)) + "%";
    }

    private static String escapeLikePattern(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
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
            case "visitorName" -> "visitorFullName";
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

    private void audit(String username, String action, String entityName, String publicRecordId,
                       String oldValues, String newValues) {
        auditService.record(username, action, entityName, publicRecordId,
                oldValues == null ? "{}" : oldValues, newValues == null ? "{}" : newValues);
    }

    private String visitSummary(Visit visit) {
        try {
            return objectMapper.writeValueAsString(VisitResponse.from(visit));
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    public static class VisitNotFoundException extends RuntimeException {
    }

    public static class InmateNotFoundException extends RuntimeException {
    }

    public static class UserNotFoundException extends RuntimeException {
    }
}
