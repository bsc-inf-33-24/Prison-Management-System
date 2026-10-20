package com.pms.api.health.service;

import com.pms.api.common.audit.AuditService;
import com.pms.api.health.dto.CreateMedicationRequest;
import com.pms.api.health.dto.MedicationResponse;
import com.pms.api.health.entity.Consultation;
import com.pms.api.health.entity.Medication;
import com.pms.api.health.repository.ConsultationRepository;
import com.pms.api.health.repository.MedicationRepository;
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
@Transactional
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final ConsultationRepository consultationRepository;
    private final AuditService auditService;

    public MedicationService(
            MedicationRepository medicationRepository,
            ConsultationRepository consultationRepository,
            AuditService auditService) {
        this.medicationRepository = medicationRepository;
        this.consultationRepository = consultationRepository;
        this.auditService = auditService;
    }

    public MedicationResponse create(
            CreateMedicationRequest request, String username) {

        if (request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Medication end date cannot be before its start date.");
        }

        Consultation consultation = consultationRepository
                .findByPublicUuid(request.consultationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Consultation record was not found."));

        String status = request.status() == null
                || request.status().isBlank()
                ? "active"
                : request.status().trim().toLowerCase(Locale.ROOT);

        if (!status.equals("active")
                && !status.equals("completed")
                && !status.equals("discontinued")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Medication status must be active, completed, or discontinued.");
        }

        Medication medication = new Medication(
                consultation,
                request.drugName().trim(),
                request.dosage(),
                request.frequency(),
                request.startDate(),
                request.endDate(),
                status,
                username
        );

        Medication saved = medicationRepository.save(medication);

        auditService.record(
                username,
                "MEDICATION_CREATED",
                "Medication",
                saved.getPublicUuid().toString(),
                null,
                "consultationId=" + consultation.getPublicUuid()
        );

        return MedicationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public MedicationResponse get(UUID id) {
        Medication medication = medicationRepository.findByPublicUuid(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Medication record was not found."));

        return MedicationResponse.from(medication);
    }

    @Transactional(readOnly = true)
    public Page<MedicationResponse> list(
            String search, Pageable pageable) {

        Specification<Medication> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        if (search != null && !search.isBlank()) {
            String pattern = "%"
                    + search.trim().toLowerCase(Locale.ROOT)
                    + "%";

            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.or(
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("drugName")),
                                            pattern),
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("status")),
                                            pattern),
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("prescribedBy")),
                                            pattern)
                            )
            );
        }

        return medicationRepository.findAll(specification, pageable)
                .map(MedicationResponse::from);
    }
}