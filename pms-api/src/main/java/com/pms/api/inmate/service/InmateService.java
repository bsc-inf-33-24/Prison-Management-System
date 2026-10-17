package com.pms.api.inmate.service;

import com.pms.api.common.audit.Audited;
import com.pms.api.inmate.dto.InmateRequest;
import com.pms.api.inmate.dto.InmateResponse;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.common.pagination.PageResponse;
import com.pms.api.common.pagination.PageableValidator;
import com.pms.api.common.pagination.PaginationProperties;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Service
public class InmateService {

    private static final String INMATE_NUMBER_CONSTRAINT = "uk_inmates_inmate_number";

    private final InmateRepository inmateRepository;
    private final String facilityName;
    private final PaginationProperties paginationProperties;

    public InmateService(InmateRepository inmateRepository,
                         @Value("${app.facility-name}") String facilityName,
                         PaginationProperties paginationProperties) {
        this.inmateRepository = inmateRepository;
        this.facilityName = facilityName;
        this.paginationProperties = paginationProperties;
    }

    @Audited
    @Transactional
    public InmateResponse enroll(InmateRequest request) {
        Inmate inmate = new Inmate(
                request.inmateNumber().trim(),
                request.firstName().trim(),
                request.lastName().trim(),
                request.dateOfBirth(),
                request.gender().trim(),
                request.admissionDate(),
                request.classification().trim(),
                facilityName
        );
        return save(inmate);
    }

    @Transactional(readOnly = true)
    public PageResponse<InmateResponse> list(String search, String classification, String gender,
                                             String facility, Integer page, Integer size, List<String> sort) {
        Pageable pageable = PageableValidator.create(page, size, sort,
                Set.of("id", "inmateNumber", "firstName", "lastName", "admissionDate", "createdAt"),
                Sort.by(Sort.Order.asc("id")), paginationProperties);
        Specification<Inmate> specification = Specification.where((Specification<Inmate>) null);
        if (search != null && !search.isBlank()) {
            String value = "%" + escapeLikePattern(search.trim()).toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), value, '!'),
                    cb.like(cb.lower(root.get("lastName")), value, '!'),
                    cb.like(cb.lower(root.get("inmateNumber")), value, '!')));
        }
        if (classification != null && !classification.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("classification"), classification.trim()));
        }
        if (gender != null && !gender.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("gender"), gender.trim()));
        }
        if (facility != null && !facility.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("currentFacility"), facility.trim()));
        }
        return PageResponse.from(inmateRepository.findAll(specification, pageable).map(InmateResponse::from));
    }

    @Transactional(readOnly = true)
    public InmateResponse get(UUID inmateId) {
        return InmateResponse.from(findInmate(inmateId));
    }

    @Audited
    @Transactional
    public InmateResponse update(UUID inmateId, InmateRequest request) {
        Inmate inmate = findInmate(inmateId);
        inmate.updateDetails(
                request.inmateNumber().trim(),
                request.firstName().trim(),
                request.lastName().trim(),
                request.dateOfBirth(),
                request.gender().trim(),
                request.admissionDate(),
                request.classification().trim()
        );
        return save(inmate);
    }

    private InmateResponse save(Inmate inmate) {
        try {
            return InmateResponse.from(inmateRepository.saveAndFlush(inmate));
        } catch (DataIntegrityViolationException exception) {
            if (violatesInmateNumberConstraint(exception)) {
                throw new InmateNumberAlreadyExistsException();
            }
            throw exception;
        }
    }

    private Inmate findInmate(UUID inmateId) {
        return inmateRepository.findByPublicUuid(inmateId).orElseThrow(InmateNotFoundException::new);
    }

    private static boolean violatesInmateNumberConstraint(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && INMATE_NUMBER_CONSTRAINT.equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }

    private static String escapeLikePattern(String search) {
        return search.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    public static class InmateNotFoundException extends RuntimeException {
    }

    public static class InmateNumberAlreadyExistsException extends RuntimeException {
    }
}
