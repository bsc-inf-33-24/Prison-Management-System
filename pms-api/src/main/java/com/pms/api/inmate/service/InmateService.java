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
import com.pms.api.notification.service.NotificationService;
import com.pms.api.prison.entity.Prison;
import com.pms.api.prison.repository.PrisonRepository;
import com.pms.api.security.PrisonAccessService;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Service
public class InmateService {

    private final InmateRepository inmateRepository;
    private final CellService cellService;
    private final PaginationProperties paginationProperties;
    private final NotificationService notificationService;
    private final PrisonRepository prisonRepository;
    private final PrisonAccessService prisonAccess;

    public InmateService(InmateRepository inmateRepository,
                         CellService cellService,
                         PaginationProperties paginationProperties,
                         NotificationService notificationService,
                         PrisonRepository prisonRepository, PrisonAccessService prisonAccess) {
        this.inmateRepository = inmateRepository;
        this.cellService = cellService;
        this.paginationProperties = paginationProperties;
        this.notificationService = notificationService;
        this.prisonRepository = prisonRepository;
        this.prisonAccess = prisonAccess;
    }

    @Audited
    @Transactional
    public InmateResponse enroll(InmateRequest request) {
        Inmate inmate = new Inmate(
                request.firstName().trim(),
                request.lastName().trim(),
                request.dateOfBirth(),
                request.gender().trim(),
                request.admissionDate(),
                request.inmateType(),
                request.bio().trim(),
                request.medicalHistory().trim(),
                request.expectedCourtDate(),
                request.nextOfKinName().trim(),
                request.nextOfKinRelationship().trim(),
                request.nextOfKinPhone().trim(),
                request.nextOfKinEmail(),
                request.contactPhone(),
                request.contactEmail()
        );
        UUID prisonId = prisonAccess.effectivePrison(null);
        Prison prison = prisonRepository.findByPublicUuid(prisonId).orElseThrow(PrisonNotFoundException::new);
        if (!prison.isActive()) throw new PrisonInactiveException();
        inmate.assignPrison(prison);
        Inmate saved = saveInmate(inmate);
        CellAllocationResponse allocation = cellService.allocate(
                saved.getPublicUuid(), new AllocateCellRequest(request.cellId()));
        notificationService.notifyRoles(
                "INMATE_REGISTERED",
                "Inmate registered",
                "A new inmate has been registered and assigned to a cell.",
                saved.getPublicUuid(),
                Set.of("Records Officer", "Duty Officer"));
        return InmateResponse.from(saved, allocation.cellId());
    }

    @Transactional(readOnly = true)
    public PageResponse<InmateResponse> list(String search, String gender,
                                             Integer page, Integer size, List<String> sort) {
        Pageable pageable = PageableValidator.create(page, size, sort,
                Set.of("id", "firstName", "lastName", "admissionDate", "createdAt"),
                Sort.by(Sort.Order.asc("id")), paginationProperties);
        UUID prisonId=prisonAccess.effectivePrison(null);
        Specification<Inmate> specification = Specification.unrestricted();
        if (prisonId!=null) specification=specification.and((root,query,cb)->cb.equal(root.get("currentPrison").get("publicUuid"),prisonId));
        if (search != null && !search.isBlank()) {
            String value = "%" + escapeLikePattern(search.trim()).toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), value, '!'),
                    cb.like(cb.lower(root.get("lastName")), value, '!')));
        }
        if (gender != null && !gender.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("gender"), gender.trim()));
        }
        return PageResponse.from(inmateRepository.findAll(specification, pageable).map(InmateResponse::from));
    }

    @Transactional(readOnly = true)
    public InmateResponse get(UUID inmateId) {
        prisonAccess.requireInmate(inmateId);
        return InmateResponse.from(findInmate(inmateId));
    }

    @Audited
    @Transactional
    public InmateResponse update(UUID inmateId, UpdateInmateDetailsRequest request) {
        Inmate inmate = findInmate(inmateId);
        prisonAccess.requirePrison(inmate.getCurrentPrison()==null?null:inmate.getCurrentPrison().getPublicUuid());
        String firstName = updateValue(request.firstName(), inmate.getFirstName());
        String lastName = updateValue(request.lastName(), inmate.getLastName());
        String gender = updateValue(request.gender(), inmate.getGender());
        inmate.updateDetails(
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
        return inmateRepository.saveAndFlush(inmate);
    }

    private Inmate findInmate(UUID inmateId) {
        return inmateRepository.findByPublicUuid(inmateId).orElseThrow(InmateNotFoundException::new);
    }

    private static String escapeLikePattern(String search) {
        return search.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    public static class InmateNotFoundException extends RuntimeException {
    }

    public static class PrisonNotFoundException extends RuntimeException { }
    public static class PrisonInactiveException extends RuntimeException { }

}
