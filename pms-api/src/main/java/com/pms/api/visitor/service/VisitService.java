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
import com.pms.api.visitor.entity.Visitor;
import com.pms.api.visitor.repository.VisitRepository;
import com.pms.api.visitor.repository.VisitorRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitService {

    private final VisitRepository visitRepository;
    private final VisitorRepository visitorRepository;
    private final InmateRepository inmateRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public VisitService(VisitRepository visitRepository,
                       VisitorRepository visitorRepository,
                       InmateRepository inmateRepository,
                       UserRepository userRepository,
                       AuditService auditService,
                       ObjectMapper objectMapper) {
        this.visitRepository = visitRepository;
        this.visitorRepository = visitorRepository;
        this.inmateRepository = inmateRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public VisitResponse recordVisit(VisitRequest request, String username) {
        if (request == null) {
            throw new IllegalArgumentException("Visit details are required.");
        }
        Visitor visitor = visitorRepository.findByPublicUuid(request.visitorId())
                .orElseThrow(VisitorService.VisitorNotFoundException::new);
        Inmate inmate = inmateRepository.findByPublicUuid(request.inmateId())
                .orElseThrow(() -> new InmateNotFoundException());
        if (request.visitDateTime() == null) {
            throw new IllegalArgumentException("visitDateTime is required.");
        }
        if (request.durationMinutes() == null || request.durationMinutes() < 0 || request.durationMinutes() > 1440) {
            throw new IllegalArgumentException("durationMinutes must be between 0 and 1440.");
        }

        User user = userRepository.findByUsername(username).orElseThrow(UserNotFoundException::new);
        Visit visit = new Visit(visitor, inmate, request.visitDateTime(), request.durationMinutes(), user);
        Visit saved = visitRepository.saveAndFlush(visit);
        audit(username, "VISIT_CREATED", "Visit", saved.getPublicUuid().toString(), "{}", visitSummary(saved));
        return VisitResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public VisitResponse getVisit(UUID visitId) {
        return VisitResponse.from(findVisit(visitId));
    }

    @Transactional(readOnly = true)
    public Page<VisitResponse> searchVisits(UUID visitId, UUID visitorId, String visitorName,
                                          String visitorIdNumber, UUID inmateId, String inmateNumber,
                                          String inmateName, OffsetDateTime visitDateFrom,
                                          OffsetDateTime visitDateTo, UUID loggedBy, int page,
                                          int size, String sort) {
        validatePage(page, size);
        if (visitDateFrom != null && visitDateTo != null && visitDateFrom.isAfter(visitDateTo)) {
            throw new IllegalArgumentException("visitDateFrom must be before or equal to visitDateTo.");
        }

        Specification<Visit> specification = Specification.where((Specification<Visit>) null);
        if (visitId != null) {
            specification = specification.and(equalLong("id", visitRepository.findByPublicUuid(visitId)
                    .map(Visit::getId).orElse(-1L)));
        }
        if (visitorId != null) {
            specification = specification.and(equalRelatedId("visitor", visitorRepository.findByPublicUuid(visitorId)
                    .map(Visitor::getId).orElse(-1L)));
        }
        if (visitorName != null && !visitorName.isBlank()) {
            specification = specification.and(visitorNameContains(visitorName));
        }
        if (visitorIdNumber != null && !visitorIdNumber.isBlank()) {
            specification = specification.and(visitorIdNumberContains(visitorIdNumber));
        }
        if (inmateId != null) {
            specification = specification.and(equalRelatedId("inmate", inmateRepository.findByPublicUuid(inmateId)
                    .map(Inmate::getId).orElse(-1L)));
        }
        if (inmateNumber != null && !inmateNumber.isBlank()) {
            specification = specification.and(inmateNumberContains(inmateNumber));
        }
        if (inmateName != null && !inmateName.isBlank()) {
            specification = specification.and(inmateNameContains(inmateName));
        }
        if (visitDateFrom != null) {
            specification = specification.and(greaterThanOrEqualTo("visitDateTime", visitDateFrom));
        }
        if (visitDateTo != null) {
            specification = specification.and(lessThanOrEqualTo("visitDateTime", visitDateTo));
        }
        if (loggedBy != null) {
            specification = specification.and(equalRelatedId("loggedBy", userRepository.findByPublicUuid(loggedBy)
                    .map(User::getId).orElse(-1L)));
        }

        Pageable pageable = PageRequest.of(page, size, parseSort(sort, "visitDateTime", Sort.Direction.DESC));
        return visitRepository.findAll(specification, pageable).map(VisitResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<VisitHistoryResponse> getInmateVisitHistory(UUID inmateId, String visitorName,
                                                          String visitorIdNumber, OffsetDateTime visitDateFrom,
                                                          OffsetDateTime visitDateTo, int page, int size, String sort) {
        Inmate inmate = inmateRepository.findByPublicUuid(inmateId).orElse(null);
        if (inmate == null) {
            throw new InmateNotFoundException();
        }
        validatePage(page, size);
        if (visitDateFrom != null && visitDateTo != null && visitDateFrom.isAfter(visitDateTo)) {
            throw new IllegalArgumentException("visitDateFrom must be before or equal to visitDateTo.");
        }

        Specification<Visit> specification = Specification.where((root, query, cb) -> cb.equal(root.get("inmate").get("id"), inmate.getId()));
        if (visitorName != null && !visitorName.isBlank()) {
            specification = specification.and(visitorNameContains(visitorName));
        }
        if (visitorIdNumber != null && !visitorIdNumber.isBlank()) {
            specification = specification.and(visitorIdNumberContains(visitorIdNumber));
        }
        if (visitDateFrom != null) {
            specification = specification.and(greaterThanOrEqualTo("visitDateTime", visitDateFrom));
        }
        if (visitDateTo != null) {
            specification = specification.and(lessThanOrEqualTo("visitDateTime", visitDateTo));
        }

        Pageable pageable = PageRequest.of(page, size, parseSort(sort, "visitDateTime", Sort.Direction.DESC));
        return visitRepository.findAll(specification, pageable).map(VisitHistoryResponse::from);
    }

    @Transactional
    public VisitResponse updateVisit(UUID visitId, UpdateVisitRequest request, String username) {
        Visit visit = findVisit(visitId);
        if (request == null || request.visitDateTime() == null) {
            throw new IllegalArgumentException("visitDateTime is required.");
        }
        if (request.durationMinutes() == null || request.durationMinutes() < 0 || request.durationMinutes() > 1440) {
            throw new IllegalArgumentException("durationMinutes must be between 0 and 1440.");
        }

        Visit prior = new Visit(visit.getVisitor(), visit.getInmate(), visit.getVisitDateTime(), visit.getDurationMinutes(), visit.getLoggedBy());
        visit.updateDetails(request.visitDateTime(), request.durationMinutes());
        Visit saved = visitRepository.saveAndFlush(visit);
        audit(username, "VISIT_UPDATED", "Visit", saved.getPublicUuid().toString(), visitSummary(prior), visitSummary(saved));
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
        String pattern = "%" + escapeLikePattern(value.trim().toLowerCase()) + "%";
        return (root, query, cb) -> {
            Join<Object, Object> visitor = root.join("visitor", JoinType.LEFT);
            return cb.like(cb.lower(visitor.get("fullName").as(String.class)), pattern, '\\');
        };
    }

    private static Specification<Visit> visitorIdNumberContains(String value) {
        String pattern = "%" + escapeLikePattern(value.trim().toLowerCase()) + "%";
        return (root, query, cb) -> {
            Join<Object, Object> visitor = root.join("visitor", JoinType.LEFT);
            return cb.like(cb.lower(visitor.get("idNumber").as(String.class)), pattern, '\\');
        };
    }

    private static Specification<Visit> inmateNumberContains(String value) {
        String pattern = "%" + escapeLikePattern(value.trim().toLowerCase()) + "%";
        return (root, query, cb) -> {
            Join<Object, Object> inmate = root.join("inmate", JoinType.LEFT);
            return cb.like(cb.lower(inmate.get("inmateNumber").as(String.class)), pattern, '\\');
        };
    }

    private static Specification<Visit> inmateNameContains(String value) {
        String pattern = "%" + escapeLikePattern(value.trim().toLowerCase()) + "%";
        return (root, query, cb) -> {
            Join<Object, Object> inmate = root.join("inmate", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(inmate.get("firstName").as(String.class)), pattern, '\\'),
                    cb.like(cb.lower(inmate.get("lastName").as(String.class)), pattern, '\\')
            );
        };
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
            case "visitorId" -> "visitor.id";
            case "inmateId" -> "inmate.id";
            case "visitDateTime" -> "visitDateTime";
            case "loggedBy" -> "loggedBy.id";
            case "visitorName" -> "visitor.fullName";
            case "visitorIdNumber" -> "visitor.idNumber";
            case "inmateNumber" -> "inmate.inmateNumber";
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

    private void audit(String username, String action, String entityName, String publicRecordId,
                       String oldValues, String newValues) {
        User user = userRepository.findByUsername(username).orElse(null);
        auditService.record(username, action, entityName, publicRecordId, oldValues == null ? "{}" : oldValues,
                newValues == null ? "{}" : newValues);
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
