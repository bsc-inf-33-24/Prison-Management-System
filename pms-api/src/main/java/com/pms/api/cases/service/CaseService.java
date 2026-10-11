package com.pms.api.cases.service;

import com.pms.api.cases.dto.CaseRequest;
import com.pms.api.cases.dto.CaseResponse;
import com.pms.api.cases.dto.CaseStatusRequest;
import com.pms.api.cases.entity.Case;
import com.pms.api.cases.entity.CaseStatus;
import com.pms.api.cases.entity.Crime;
import com.pms.api.cases.repository.CaseRepository;
import com.pms.api.cases.repository.CrimeRepository;
import com.pms.api.common.audit.Audited;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseService {

    private final CaseRepository caseRepository;
    private final InmateRepository inmateRepository;
    private final CrimeRepository crimeRepository;
    private final UserRepository userRepository;

    public CaseService(CaseRepository caseRepository,
                       InmateRepository inmateRepository,
                       CrimeRepository crimeRepository,
                       UserRepository userRepository) {
        this.caseRepository = caseRepository;
        this.inmateRepository = inmateRepository;
        this.crimeRepository = crimeRepository;
        this.userRepository = userRepository;
    }

    @Audited
    @Transactional
    public CaseResponse createCase(CaseRequest request, String username) {
        validateRequest(request);

        Inmate inmate = inmateRepository.findById(request.inmateId())
                .orElseThrow(InmateNotFoundException::new);

        List<Crime> crimes = getValidCrimes(request.crimeIds());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));

        Case newCase = new Case(inmate, request.caseNumber().trim(), request.court().trim(),
                request.caseStartDate(), request.caseEndDate(), user);
        newCase.replaceCrimes(new LinkedHashSet<>(crimes));

        return CaseResponse.from(caseRepository.saveAndFlush(newCase));
    }

    @Transactional(readOnly = true)
    public CaseResponse getCase(Long caseId) {
        return CaseResponse.from(findCase(caseId));
    }

    @Transactional(readOnly = true)
    public List<CaseResponse> listCases(String status, Long inmateId, Long crimeId) {
        return caseRepository.findAllByOrderByCaseStartDateDesc().stream()
                .filter(c -> status == null || c.getStatus().name().equalsIgnoreCase(status))
                .filter(c -> inmateId == null || c.getInmate().getId().equals(inmateId))
                .filter(c -> crimeId == null || c.getCrimes().stream().anyMatch(crime -> crime.getId().equals(crimeId)))
                .map(CaseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CaseResponse> getCasesForInmate(Long inmateId) {
        return caseRepository.findByInmateIdOrderByCaseStartDateDesc(inmateId).stream()
                .map(CaseResponse::from)
                .toList();
    }

    @Audited
    @Transactional
    public CaseResponse updateCase(Long caseId, CaseRequest request) {
        Case existing = findCase(caseId);
        validateRequest(request);

        existing.updateDetails(request.caseNumber().trim(), request.court().trim(),
                request.caseStartDate(), request.caseEndDate());

        existing.replaceCrimes(new LinkedHashSet<>(getValidCrimes(request.crimeIds())));
        return CaseResponse.from(caseRepository.saveAndFlush(existing));
    }

    @Audited
    @Transactional
    public CaseResponse updateStatus(Long caseId, CaseStatusRequest request) {
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

    private List<Crime> getValidCrimes(Set<Long> crimeIds) {
        if (crimeIds == null || crimeIds.isEmpty()) {
            return List.of();
        }

        if (crimeIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new InvalidCaseRequestException();
        }

        List<Crime> crimes = crimeRepository.findAllById(crimeIds);
        if (crimes.size() != crimeIds.size()) {
            throw new CrimeNotFoundException();
        }

        return crimes;
    }

    private static void validateRequest(CaseRequest request) {
        if (request == null || request.inmateId() == null || request.caseNumber() == null
                || request.caseNumber().isBlank() || request.court() == null || request.court().isBlank()
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

    private Case findCase(Long caseId) {
        return caseRepository.findById(caseId).orElseThrow(CaseNotFoundException::new);
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
