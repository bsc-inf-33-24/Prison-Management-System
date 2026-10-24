package com.pms.api.health.service;

import com.pms.api.common.audit.AuditService;
import com.pms.api.health.dto.CreateMedicalConditionRequest;
import com.pms.api.health.dto.MedicalConditionResponse;
import com.pms.api.health.entity.HealthRecord;
import com.pms.api.health.entity.MedicalCondition;
import com.pms.api.health.repository.HealthRecordRepository;
import com.pms.api.health.repository.MedicalConditionRepository;
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
public class MedicalConditionService {

    private final MedicalConditionRepository conditionRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final AuditService auditService;

    public MedicalConditionService(
            MedicalConditionRepository conditionRepository,
            HealthRecordRepository healthRecordRepository,
            AuditService auditService) {
        this.conditionRepository = conditionRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.auditService = auditService;
    }

    @Transactional
    public MedicalConditionResponse create(
            CreateMedicalConditionRequest request, String username) {

        HealthRecord healthRecord =
                healthRecordRepository.findByPublicUuid(request.healthRecordId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Health record not found."));

        String status = request.status() == null || request.status().isBlank()
                ? "active"
                : request.status().trim().toLowerCase(Locale.ROOT);

        if (!status.equals("active") && !status.equals("resolved")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be active or resolved.");
        }

        MedicalCondition condition = new MedicalCondition(
                healthRecord,
                request.conditionType().trim(),
                request.conditionName().trim(),
                request.severity(),
                request.dateRecorded(),
                status,
                username);

        MedicalCondition saved = conditionRepository.save(condition);

        // Never put the condition name, diagnosis, or other medical details in audit data.
        auditService.record(
                username,
                "MEDICAL_CONDITION_CREATED",
                "MedicalCondition",
                saved.getPublicUuid().toString(),
                null,
                "healthRecordId=" + healthRecord.getPublicUuid());

        return MedicalConditionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public MedicalConditionResponse get(UUID id) {
        MedicalCondition condition = conditionRepository.findByPublicUuid(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Medical condition not found."));

        return MedicalConditionResponse.from(condition);
    }

    @Transactional(readOnly = true)
    public Page<MedicalConditionResponse> list(
            String search, Pageable pageable) {

        Specification<MedicalCondition> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        if (search != null && !search.isBlank()) {
            String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";

            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.or(
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("conditionType")),
                                    term),
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("conditionName")),
                                    term),
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("severity")),
                                    term),
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("status")),
                                    term)
                    ));
        }

        return conditionRepository.findAll(specification, pageable)
                .map(MedicalConditionResponse::from);
    }
}
