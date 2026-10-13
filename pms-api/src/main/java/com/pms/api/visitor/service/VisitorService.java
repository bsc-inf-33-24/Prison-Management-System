package com.pms.api.visitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.api.common.audit.AuditService;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import com.pms.api.visitor.dto.VisitorRequest;
import com.pms.api.visitor.dto.VisitorResponse;
import com.pms.api.visitor.entity.Visitor;
import com.pms.api.visitor.repository.VisitorRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitorService {

    private final VisitorRepository visitorRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final InmateRepository inmateRepository;

    public VisitorService(VisitorRepository visitorRepository,
                          UserRepository userRepository,
                          AuditService auditService,
                          ObjectMapper objectMapper,
                          InmateRepository inmateRepository) {
        this.visitorRepository = visitorRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.inmateRepository = inmateRepository;
    }

    @Transactional
    public VisitorResponse registerVisitor(VisitorRequest request, String username) {
        String fullName = request.fullName() == null ? "" : request.fullName().trim();
        String idNumber = request.idNumber() == null ? "" : request.idNumber().trim();
        String relationshipToInmate = request.relationshipToInmate() == null ? "" : request.relationshipToInmate().trim();
        String phone = request.phone() == null ? "" : request.phone().trim();

        if (fullName.isBlank() || idNumber.isBlank() || relationshipToInmate.isBlank()) {
            throw new IllegalArgumentException("Visitor details are required.");
        }
        if (visitorRepository.existsByIdNumber(idNumber)) {
            throw new DuplicateVisitorIdentityException();
        }

        Visitor visitor = new Visitor(fullName, idNumber, phone, relationshipToInmate);
        Visitor saved = visitorRepository.saveAndFlush(visitor);
        audit(username, "VISITOR_CREATED", "Visitor", saved.getId(), null, visitorSummary(saved));
        return VisitorResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public VisitorResponse getVisitor(Long visitorId) {
        return VisitorResponse.from(findVisitor(visitorId));
    }

    @Transactional(readOnly = true)
    public Page<VisitorResponse> searchVisitors(String fullName, String idNumber, String phone,
                                              String relationshipToInmate, int page, int size, String sort) {
        validatePage(page, size);

        Specification<Visitor> specification = Specification.where((Specification<Visitor>) null);
        if (fullName != null && !fullName.isBlank()) {
            specification = specification.and(likeIgnoreCase("fullName", fullName));
        }
        if (idNumber != null && !idNumber.isBlank()) {
            specification = specification.and(likeIgnoreCase("idNumber", idNumber));
        }
        if (phone != null && !phone.isBlank()) {
            specification = specification.and(likeIgnoreCase("phone", phone));
        }
        if (relationshipToInmate != null && !relationshipToInmate.isBlank()) {
            specification = specification.and(likeIgnoreCase("relationshipToInmate", relationshipToInmate));
        }

        Pageable pageable = PageRequest.of(page, size, parseSort(sort, "fullName", Sort.Direction.ASC));
        return visitorRepository.findAll(specification, pageable).map(VisitorResponse::from);
    }

    public Visitor findVisitor(Long visitorId) {
        return visitorRepository.findById(visitorId).orElseThrow(VisitorNotFoundException::new);
    }

    private static Specification<Visitor> likeIgnoreCase(String fieldName, String value) {
        String safeValue = escapeLikePattern(value.trim());
        return (root, query, cb) -> {
            String pattern = "%" + safeValue.toLowerCase() + "%";
            return cb.like(cb.lower(root.get(fieldName).as(String.class)), pattern, '\\');
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
        String dir = parts.length > 1 ? parts[1].trim() : defaultDirection.name();
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        String normalizedField = switch (field) {
            case "fullName" -> "fullName";
            case "idNumber" -> "idNumber";
            case "phone" -> "phone";
            case "relationshipToInmate" -> "relationshipToInmate";
            default -> defaultField;
        };
        return Sort.by(direction, normalizedField);
    }

    private static void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative.");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100.");
        }
    }

    private void audit(String username, String action, String entityName, Long recordId,
                       String oldValues, String newValues) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = userRepository.findByUsername("system").orElse(null);
        }
        auditService.record(username, action, entityName, recordId, oldValues == null ? "{}" : oldValues,
                newValues == null ? "{}" : newValues);
    }

    private String visitorSummary(Visitor visitor) {
        try {
            return objectMapper.writeValueAsString(new VisitorResponse(
                    visitor.getId(),
                    visitor.getFullName(),
                    visitor.getIdNumber(),
                    visitor.getPhone(),
                    visitor.getRelationshipToInmate(),
                    visitor.getCreatedAt(),
                    visitor.getUpdatedAt()
            ));
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    public static class VisitorNotFoundException extends RuntimeException {
    }

    public static class DuplicateVisitorIdentityException extends RuntimeException {
    }
}
