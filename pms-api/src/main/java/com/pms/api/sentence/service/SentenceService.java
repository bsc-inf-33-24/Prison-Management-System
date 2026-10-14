package com.pms.api.sentence.service;

import com.pms.api.cases.entity.Case;
import com.pms.api.cases.repository.CaseRepository;
import com.pms.api.common.audit.Audited;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.sentence.dto.SentenceRequest;
import com.pms.api.sentence.dto.SentenceResponse;
import com.pms.api.sentence.entity.Sentence;
import com.pms.api.sentence.repository.SentenceRepository;
import com.pms.api.common.pagination.PageResponse;
import com.pms.api.common.pagination.PageableValidator;
import com.pms.api.common.pagination.PaginationProperties;
import java.util.List;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SentenceService {

    private final SentenceRepository sentenceRepository;
    private final InmateRepository inmateRepository;
    private final CaseRepository caseRepository;
    private final PaginationProperties paginationProperties;

    public SentenceService(SentenceRepository sentenceRepository,
                          InmateRepository inmateRepository,
                          CaseRepository caseRepository,
                          PaginationProperties paginationProperties) {
        this.sentenceRepository = sentenceRepository;
        this.inmateRepository = inmateRepository;
        this.caseRepository = caseRepository;
        this.paginationProperties = paginationProperties;
    }

    @Audited
    @Transactional
    public SentenceResponse create(SentenceRequest request) {
        validateRequest(request);
        Case caseEntity = findCase(request.caseId());
        Inmate inmate = findInmate(request.inmateId());
        ensureCaseAndInmateMatch(caseEntity, inmate);
        ensureSentenceAllowedForCase(caseEntity);
        Sentence sentence = Sentence.create(
                caseEntity,
                inmate,
                request.duration(),
                request.startDate(),
                request.releaseDate(),
                request.status()
        );
        return SentenceResponse.from(sentenceRepository.saveAndFlush(sentence));
    }

    @Transactional(readOnly = true)
    public PageResponse<SentenceResponse> list(Long inmateId, Long caseId, String status,
                                                LocalDate startDateFrom, LocalDate startDateTo,
                                                Integer page, Integer size, List<String> sort) {
        Pageable pageable = PageableValidator.create(page, size, sort,
                Set.of("id", "startDate", "releaseDate", "status", "createdAt"),
                Sort.by(Sort.Order.asc("id")), paginationProperties);
        if (startDateFrom != null && startDateTo != null && startDateFrom.isAfter(startDateTo)) {
            throw new IllegalArgumentException("The start date range is invalid.");
        }
        Specification<Sentence> specification = Specification.where(null);
        if (inmateId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("inmate").get("id"), inmateId));
        }
        if (caseId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("caseEntity").get("id"), caseId));
        }
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toLowerCase(Locale.ROOT);
            if (!Sentence.ALLOWED_STATUSES.contains(normalized)) {
                throw new IllegalArgumentException("Invalid sentence status.");
            }
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), normalized));
        }
        if (startDateFrom != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), startDateFrom));
        }
        if (startDateTo != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), startDateTo));
        }
        return PageResponse.from(sentenceRepository.findAll(specification, pageable).map(SentenceResponse::from));
    }

    @Transactional(readOnly = true)
    public SentenceResponse get(Long sentenceId) {
        return SentenceResponse.from(findSentence(sentenceId));
    }

    @Audited
    @Transactional
    public SentenceResponse update(Long sentenceId, SentenceRequest request) {
        validateRequest(request);
        Sentence sentence = findSentence(sentenceId);
        Case caseEntity = findCase(request.caseId());
        Inmate inmate = findInmate(request.inmateId());
        ensureCaseAndInmateMatch(caseEntity, inmate);
        ensureSentenceAllowedForCase(caseEntity);
        sentence.updateDetails(
                caseEntity,
                inmate,
                request.duration(),
                request.startDate(),
                request.releaseDate(),
                request.status()
        );
        return SentenceResponse.from(sentenceRepository.saveAndFlush(sentence));
    }

    private void validateRequest(SentenceRequest request) {
        if (request == null) {
            throw new InvalidSentenceRequestException();
        }
        if (request.caseId() == null || request.caseId() <= 0) {
            throw new InvalidSentenceRequestException();
        }
        if (request.inmateId() == null || request.inmateId() <= 0) {
            throw new InvalidSentenceRequestException();
        }
        if (request.duration() == null || request.duration() <= 0) {
            throw new InvalidSentenceRequestException();
        }
        if (request.startDate() == null) {
            throw new InvalidSentenceRequestException();
        }
        if (request.releaseDate() != null && request.releaseDate().isBefore(request.startDate())) {
            throw new InvalidSentenceRequestException();
        }
        if (request.status() == null || request.status().isBlank()) {
            throw new InvalidSentenceRequestException();
        }
    }

    private void ensureCaseAndInmateMatch(Case caseEntity, Inmate inmate) {
        if (caseEntity.getInmate() == null || !caseEntity.getInmate().getId().equals(inmate.getId())) {
            throw new InvalidSentenceRequestException();
        }
    }

    private void ensureSentenceAllowedForCase(Case caseEntity) {
        if (caseEntity.getStatus() != null && caseEntity.getStatus() != com.pms.api.cases.entity.CaseStatus.ONGOING) {
            throw new InvalidSentenceRequestException();
        }
    }

    private Case findCase(Long caseId) {
        return caseRepository.findById(caseId).orElseThrow(CaseNotFoundException::new);
    }

    private Inmate findInmate(Long inmateId) {
        return inmateRepository.findById(inmateId).orElseThrow(InmateNotFoundException::new);
    }

    private Sentence findSentence(Long sentenceId) {
        return sentenceRepository.findById(sentenceId).orElseThrow(SentenceNotFoundException::new);
    }

    public static class SentenceNotFoundException extends RuntimeException {
    }

    public static class InmateNotFoundException extends RuntimeException {
    }

    public static class CaseNotFoundException extends RuntimeException {
    }

    public static class InvalidSentenceRequestException extends RuntimeException {
    }
}
