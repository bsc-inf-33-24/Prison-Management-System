package com.pms.api.health.service;

import com.pms.api.common.audit.AuditService;
import com.pms.api.health.dto.ConsultationResponse;
import com.pms.api.health.dto.CreateConsultationRequest;
import com.pms.api.health.entity.Consultation;
import com.pms.api.health.entity.HealthRecord;
import com.pms.api.health.repository.ConsultationRepository;
import com.pms.api.health.repository.HealthRecordRepository;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final AuditService auditService;

    public ConsultationService(
            ConsultationRepository consultationRepository,
            HealthRecordRepository healthRecordRepository,
            AuditService auditService) {
        this.consultationRepository = consultationRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ConsultationResponse create(
            CreateConsultationRequest request, String username) {

        if (request.consultationDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Consultation date cannot be in the future.");
        }

        HealthRecord healthRecord =
                healthRecordRepository.findByPublicUuid(request.healthRecordId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Health record not found."));

        Consultation consultation = new Consultation(
                healthRecord,
                request.consultationDate(),
                request.complaint(),
                request.diagnosis(),
                request.treatmentProvided(),
                username);

        Consultation saved = consultationRepository.save(consultation);

        // Do not put complaints, diagnoses, or treatment details in the audit log.
        auditService.record(
                username,
                "CONSULTATION_CREATED",
                "Consultation",
                saved.getPublicUuid().toString(),
                null,
                "healthRecordId=" + healthRecord.getPublicUuid());

        return ConsultationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ConsultationResponse get(UUID id) {
        Consultation consultation =
                consultationRepository.findByPublicUuid(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Consultation not found."));

        return ConsultationResponse.from(consultation);
    }

    @Transactional(readOnly = true)
    public Page<ConsultationResponse> list(
            String search, Pageable pageable) {

        Specification<Consultation> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        if (search != null && !search.isBlank()) {
            String term =
                    "%" + search.trim().toLowerCase(Locale.ROOT) + "%";

            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.or(
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("complaint")),
                                            term),
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("diagnosis")),
                                            term),
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("treatmentProvided")),
                                            term),
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("consultedBy")),
                                            term)
                            ));
        }

        return consultationRepository.findAll(specification, pageable)
                .map(ConsultationResponse::from);
    }
}