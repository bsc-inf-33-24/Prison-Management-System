package com.pms.api.health.service;

import com.pms.api.common.audit.AuditService;
import com.pms.api.health.dto.CreateHealthRecordRequest;
import com.pms.api.health.dto.HealthRecordResponse;
import com.pms.api.health.entity.HealthRecord;
import com.pms.api.health.repository.HealthRecordRepository;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
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
public class HealthRecordService {

    private final HealthRecordRepository healthRecordRepository;
    private final InmateRepository inmateRepository;
    private final AuditService auditService;

    public HealthRecordService(
            HealthRecordRepository healthRecordRepository,
            InmateRepository inmateRepository,
            AuditService auditService) {
        this.healthRecordRepository = healthRecordRepository;
        this.inmateRepository = inmateRepository;
        this.auditService = auditService;
    }

    @Transactional
    public HealthRecordResponse create(
            CreateHealthRecordRequest request, String username) {

        Inmate inmate = inmateRepository.findByPublicUuid(request.inmateId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Inmate not found."));

        String status = request.status() == null || request.status().isBlank()
                ? "ongoing"
                : request.status().trim().toLowerCase(Locale.ROOT);

        if (!status.equals("ongoing")
                && !status.equals("critical")
                && !status.equals("recovered")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be ongoing, critical, or recovered.");
        }

        HealthRecord record = new HealthRecord(
                inmate,
                request.screeningDate(),
                request.screeningResults(),
                request.bloodGroup(),
                status);

        HealthRecord saved = healthRecordRepository.save(record);

        // Record the action without including sensitive medical details.
        auditService.record(
                username,
                "HEALTH_RECORD_CREATED",
                "HealthRecord",
                saved.getPublicUuid().toString(),
                null,
                "inmateId=" + inmate.getPublicUuid());

        return HealthRecordResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public HealthRecordResponse get(UUID id) {
        HealthRecord record = healthRecordRepository.findByPublicUuid(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Health record not found."));

        return HealthRecordResponse.from(record);
    }

    @Transactional(readOnly = true)
    public Page<HealthRecordResponse> list(
            String search, Pageable pageable) {

        // Start with a specification that matches all health records.
        Specification<HealthRecord> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        if (search != null && !search.isBlank()) {
            String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";

            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.or(
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(
                                            root.get("screeningResults")),
                                    term),
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(root.get("status")),
                                    term),
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(
                                            root.join("inmate").get("inmateNumber")),
                                    term),
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(
                                            root.join("inmate").get("firstName")),
                                    term),
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(
                                            root.join("inmate").get("lastName")),
                                    term)
                    ));
        }

        return healthRecordRepository.findAll(specification, pageable)
                .map(HealthRecordResponse::from);
    }
}
