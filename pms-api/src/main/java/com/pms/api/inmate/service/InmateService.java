package com.pms.api.inmate.service;

import com.pms.api.cell.dto.AllocateCellRequest;
import com.pms.api.cell.dto.CellAllocationResponse;
import com.pms.api.cell.service.CellService;
import com.pms.api.common.audit.Audited;
import com.pms.api.inmate.dto.InmateRequest;
import com.pms.api.inmate.dto.InmateResponse;
import com.pms.api.inmate.dto.UpdateInmateDetailsRequest;
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
    private final CellService cellService;
    private final PaginationProperties paginationProperties;

    public InmateService(InmateRepository inmateRepository,
                         CellService cellService,
                         PaginationProperties paginationProperties) {
        this.inmateRepository = inmateRepository;
        this.cellService = cellService;
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
                request.offence() == null ? null : request.offence().trim(),
                request.sentenceStartDate(),
                request.sentenceEndDate()
        );
        Inmate saved = saveInmate(inmate);
        CellAllocationResponse allocation = cellService.allocate(
                saved.getPublicUuid(), new AllocateCellRequest(request.cellId()));
        return InmateResponse.from(saved, allocation.cellId());
    }

    @Transactional(readOnly = true)
    public PageResponse<InmateResponse> list(String search, String gender,
                                             Integer page, Integer size, List<String> sort) {
        Pageable pageable = PageableValidator.create(page, size, sort,
                Set.of("id", "inmateNumber", "firstName", "lastName", "admissionDate", "createdAt"),
                Sort.by(Sort.Order.asc("id")), paginationProperties);
        Specification<Inmate> specification = Specification.unrestricted();
        if (search != null && !search.isBlank()) {
            String value = "%" + escapeLikePattern(search.trim()).toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), value, '!'),
                    cb.like(cb.lower(root.get("lastName")), value, '!'),
                    cb.like(cb.lower(root.get("inmateNumber")), value, '!')));
        }
        if (gender != null && !gender.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("gender"), gender.trim()));
        }
        return PageResponse.from(inmateRepository.findAll(specification, pageable).map(InmateResponse::from));
    }

    @Transactional(readOnly = true)
    public InmateResponse get(UUID inmateId) {
        return InmateResponse.from(findInmate(inmateId));
    }

    @Audited
    @Transactional
    public InmateResponse update(UUID inmateId, UpdateInmateDetailsRequest request) {
        Inmate inmate = findInmate(inmateId);
        String inmateNumber = updateValue(request.inmateNumber(), inmate.getInmateNumber());
        String firstName = updateValue(request.firstName(), inmate.getFirstName());
        String lastName = updateValue(request.lastName(), inmate.getLastName());
        String gender = updateValue(request.gender(), inmate.getGender());
        inmate.updateDetails(
                inmateNumber,
                firstName,
                lastName,
                request.dateOfBirth() == null ? inmate.getDateOfBirth() : request.dateOfBirth(),
                gender,
                request.admissionDate() == null ? inmate.getAdmissionDate() : request.admissionDate()
        );
        return InmateResponse.from(saveInmate(inmate));
    }

    private static String updateValue(String requestedValue, String currentValue) {
        if (requestedValue == null) {
            return currentValue;
        }
        String normalized = requestedValue.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Updated text fields must not be blank.");
        }
        return normalized;
    }

    private Inmate saveInmate(Inmate inmate) {
        try {
            return inmateRepository.saveAndFlush(inmate);
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
