package com.pms.api.health.service;

import com.pms.api.common.audit.AuditService;
import com.pms.api.health.dto.AllergyResponse;
import com.pms.api.health.dto.CreateAllergyRequest;
import com.pms.api.health.entity.Allergy;
import com.pms.api.health.repository.AllergyRepository;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
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
public class AllergyService {

    private final AllergyRepository allergyRepository;
    private final InmateRepository inmateRepository;
    private final AuditService auditService;

    public AllergyService(
            AllergyRepository allergyRepository,
            InmateRepository inmateRepository,
            AuditService auditService) {
        this.allergyRepository = allergyRepository;
        this.inmateRepository = inmateRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AllergyResponse create(CreateAllergyRequest request, String username) {
        Inmate inmate = inmateRepository.findByPublicUuid(request.inmateId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Inmate not found."));

        LocalDate recordedDate = request.recordedDate();
        if (recordedDate.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Recorded date cannot be in the future.");
        }

        String status = request.status() == null || request.status().isBlank()
                ? "active"
                : request.status().trim().toLowerCase(Locale.ROOT);

        if (!status.equals("active") && !status.equals("resolved")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be active or resolved.");
        }

        Allergy allergy = new Allergy(
                inmate,
                request.allergen().trim(),
                request.reaction(),
                request.severity(),
                recordedDate,
                status);

        Allergy saved = allergyRepository.save(allergy);

        // Do not include allergen names or reactions in audit data.
        auditService.record(
                username,
                "ALLERGY_CREATED",
                "Allergy",
                saved.getPublicUuid().toString(),
                null,
                "inmateId=" + inmate.getPublicUuid());

        return AllergyResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AllergyResponse get(UUID id) {
        Allergy allergy = allergyRepository.findByPublicUuid(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Allergy not found."));

        return AllergyResponse.from(allergy);
    }

    @Transactional(readOnly = true)
    public Page<AllergyResponse> list(String search, Pageable pageable) {
        Specification<Allergy> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        if (search != null && !search.isBlank()) {
            String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";

            specification = specification.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("allergen")), term),
                            cb.like(cb.lower(root.get("severity")), term),
                            cb.like(cb.lower(root.get("status")), term)
                    ));
        }

        return allergyRepository.findAll(specification, pageable)
                .map(AllergyResponse::from);
    }
}