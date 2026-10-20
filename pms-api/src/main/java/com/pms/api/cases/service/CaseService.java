package com.pms.api.cases.service;

import com.pms.api.cases.dto.CaseRequest;
import com.pms.api.cases.dto.CaseResponse;
import com.pms.api.cases.dto.CaseStatusRequest;
import com.pms.api.cases.dto.UpdateCaseDetailsRequest;
import com.pms.api.cases.entity.Case;
import com.pms.api.cases.entity.CaseStatus;
import com.pms.api.cases.repository.CaseRepository;
import com.pms.api.common.audit.Audited;
import com.pms.api.common.pagination.PageResponse;
import com.pms.api.common.pagination.PageableValidator;
import com.pms.api.common.pagination.PaginationProperties;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseService {

    private final CaseRepository caseRepository;
    private final InmateRepository inmateRepository;
    private final UserRepository userRepository;
    private final PaginationProperties paginationProperties;

    public CaseService(CaseRepository caseRepository,
                       InmateRepository inmateRepository,
                       UserRepository userRepository,
                       PaginationProperties paginationProperties) {
        this.caseRepository = caseRepository;
        this.inmateRepository = inmateRepository;
        this.userRepository = userRepository;
        this.paginationProperties = paginationProperties;
    }

    @Audited
    @Transactional
    public CaseResponse createCase(CaseRequest request, String username) {
        validateRequest(request);

        Inmate inmate = inmateRepository.findByPublicUuid(request.inmateId())
                .orElseThrow(InmateNotFoundException::new);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));

        Case newCase = new Case(inmate, request.caseNumber().trim(), request.court().trim(),request.crimes(),
                request.caseStartDate(), request.caseEndDate(), user);

        return CaseResponse.from(caseRepository.saveAndFlush(newCase));
    }

    @Transactional(readOnly = true)
    public CaseResponse getCase(UUID caseId) {
        return CaseResponse.from(findCase(caseId));
    }

    @Transactional(readOnly = true)
    public PageResponse<CaseResponse> listCases(String status, UUID inmateId, LocalDate caseStartDateFrom, LocalDate caseStartDateTo,
                                                Integer page, Integer size, List<String> sort) {
        Pageable pageable = PageableValidator.create(page, size, sort,
                    Set.of("id", "caseNumber", "caseStartDate", "caseEndDate", "status"),
                    Sort.by(Sort.Order.desc("caseStartDate"), Sort.Order.desc("id")), paginationProperties);

        Long internalInmateId = inmateId == null ? null : inmateRepository.findByPublicUuid(inmateId).map(Inmate::getId).orElse(-1L);
        
        Specification<Case> specification = caseSpecification(status, internalInmateId,
                caseStartDateFrom, caseStartDateTo);
        return PageResponse.from(caseRepository.findAll(specification, pageable).map(CaseResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<CaseResponse> getCasesForInmate(UUID inmateId, String status,
                                                        Integer page, Integer size, List<String> sort) {
        return listCases(status, inmateId, null, null, page, size, sort);
    }

    private static Specification<Case> caseSpecification(String status, Long inmateId, LocalDate startDateFrom, LocalDate startDateTo) {
        if (startDateFrom != null && startDateTo != null && startDateFrom.isAfter(startDateTo)) {
            throw new IllegalArgumentException("The case start date range is invalid.");
        }
        Specification<Case> specification = Specification.where((Specification<Case>) null);
        if (status != null && !status.isBlank()) {
            CaseStatus caseStatus = CaseStatus.from(status);
            if (caseStatus == null) {
                throw new IllegalArgumentException("Invalid case status.");
            }
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), caseStatus));
        }
        if (inmateId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("inmate").get("id"), inmateId));
        }
        
        if (startDateFrom != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("caseStartDate"), startDateFrom));
        }
        if (startDateTo != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("caseStartDate"), startDateTo));
        }
        return specification;
    }

    @Audited
    @Transactional
    public CaseResponse updateCase(UUID caseId, UpdateCaseDetailsRequest request) {
        Case existing = findCase(caseId);
        if (request == null) {
            throw new InvalidCaseRequestException();
        }
        String caseNumber = updateText(request.caseNumber(), existing.getCaseNumber());
        String court = updateText(request.court(), existing.getCourt());
        String crimes = updateText(request.crimes(), existing.getCrimes());
        LocalDate startDate = request.caseStartDate() == null ? existing.getCaseStartDate() : request.caseStartDate();
        LocalDate endDate = request.caseEndDate() == null ? existing.getCaseEndDate() : request.caseEndDate();
        if (startDate.isAfter(endDate)) {
            throw new InvalidCaseRequestException();
        }

        existing.updateDetails(caseNumber, court, crimes, startDate, endDate);
        
        return CaseResponse.from(caseRepository.saveAndFlush(existing));
    }

    private static String updateText(String value, String currentValue) {
        if (value == null) {
            return currentValue;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new InvalidCaseRequestException();
        }
        return normalized;
    }

    @Audited
    @Transactional
    public CaseResponse updateStatus(UUID caseId, CaseStatusRequest request) {
        if (request == null || request.status() == null || request.status().isBlank()) {
            throw new InvalidCaseRequestException();
        }

        Case existing = findCase(caseId);
        CaseStatus nextStatus = CaseStatus.from(request.status());

        if (nextStatus == null || !isValidTransition(existing.getStatus(), nextStatus)) {
            throw new InvalidCaseRequestException();
        }

        if ((nextStatus == CaseStatus.CLOSED || nextStatus == CaseStatus.APPEALED)
                && (request.reason() == null || request.reason().isBlank())) {
            throw new InvalidCaseRequestException();
        }

        existing.setStatus(nextStatus);
        return CaseResponse.from(caseRepository.saveAndFlush(existing));
    }

    private static void validateRequest(CaseRequest request) {
        if (request == null || request.inmateId() == null || request.caseNumber() == null
                || request.caseNumber().isBlank() || request.court() == null || request.court().isBlank() 
                || request.crimes() == null || request.crimes().isBlank()
                || request.caseStartDate() == null || request.caseEndDate() == null) {
            throw new InvalidCaseRequestException();
        }

        if (request.caseStartDate().isAfter(request.caseEndDate())) {
            throw new InvalidCaseRequestException();
        }
    }

    private static boolean isValidTransition(CaseStatus current, CaseStatus next) {
        if (current == CaseStatus.ONGOING) {
            return next == CaseStatus.CLOSED || next == CaseStatus.APPEALED;
        }
        if (current == CaseStatus.APPEALED) {
            return next == CaseStatus.ONGOING || next == CaseStatus.CLOSED;
        }
        return false;
    }

    private Case findCase(UUID caseId) {
        return caseRepository.findByPublicUuid(caseId).orElseThrow(CaseNotFoundException::new);
    }

    public static class CaseNotFoundException extends RuntimeException {
    }

    public static class CrimeNotFoundException extends RuntimeException {
    }

    public static class InmateNotFoundException extends RuntimeException {
    }

    public static class InvalidCaseRequestException extends RuntimeException {
    }
}
