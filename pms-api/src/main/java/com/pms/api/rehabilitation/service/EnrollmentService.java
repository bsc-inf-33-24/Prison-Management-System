package com.pms.api.rehabilitation.service;

import com.pms.api.common.audit.AuditService;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.repository.InmateRepository;
import com.pms.api.inmate.service.InmateService;
import com.pms.api.rehabilitation.dto.enrollment.CreateEnrollmentRequest;
import com.pms.api.rehabilitation.dto.enrollment.EnrollmentResponse;
import com.pms.api.rehabilitation.entity.Enrollment;
import com.pms.api.rehabilitation.entity.RehabilitationProgram;
import com.pms.api.rehabilitation.exception.RehabilitationExceptions.DuplicateEnrollmentException;
import com.pms.api.rehabilitation.exception.RehabilitationExceptions.EnrollmentNotFoundException;
import com.pms.api.rehabilitation.exception.RehabilitationExceptions.ProgramNotActiveException;
import com.pms.api.rehabilitation.repository.EnrollmentRepository;
import com.pms.api.rehabilitation.specification.EnrollmentSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class EnrollmentService {

    public static final String ENROLLED = "enrolled";
    public static final String COMPLETED = "completed";
    public static final String WITHDRAWN = "withdrawn";
    public static final String SUSPENDED = "suspended";

    private final EnrollmentRepository enrollmentRepository;
    private final InmateRepository inmateRepository;
    private final RehabilitationProgramService programService;
    private final AuditService auditService;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                              InmateRepository inmateRepository,
                              RehabilitationProgramService programService,
                              AuditService auditService) {
        this.enrollmentRepository = enrollmentRepository;
        this.inmateRepository = inmateRepository;
        this.programService = programService;
        this.auditService = auditService;
    }

    @Transactional
    public EnrollmentResponse create(CreateEnrollmentRequest request, String username) {
        Inmate inmate = inmateRepository.findByPublicUuid(request.inmateId())
                .orElseThrow(InmateService.InmateNotFoundException::new);
        RehabilitationProgram program = programService.findProgram(request.programId());

        if (!RehabilitationProgramService.ACTIVE.equals(program.getStatus())) {
            throw new ProgramNotActiveException();
        }

        if (enrollmentRepository.existsByInmate_IdAndProgram_IdAndStatusIn(
                inmate.getId(), program.getId(), List.of(ENROLLED, SUSPENDED))) {
            throw new DuplicateEnrollmentException();
        }

        Enrollment enrollment = new Enrollment(inmate, program, request.enrollmentDate(), ENROLLED);
        Enrollment saved = enrollmentRepository.save(enrollment);
        auditService.record(username, "ENROLLMENT_CREATED", "Enrollment", saved.getPublicUuid().toString(), null,
                "inmateId=" + inmate.getPublicUuid() + ",programId=" + program.getPublicUuid());
        return EnrollmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> list(Pageable pageable) {
        return list(null, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> list(UUID inmateId, String inmateName, UUID programId,
                                         String programName, String status, Pageable pageable) {
        Specification<Enrollment> specification =
                Specification.unrestricted();
        Long internalInmateId = inmateId == null ? null : inmateRepository.findByPublicUuid(inmateId).map(Inmate::getId).orElse(-1L);
        Long internalProgramId = programId == null ? null : programService.findProgram(programId).getId();
        if (internalInmateId != null) {
            specification = specification.and(EnrollmentSpecifications.inmateId(internalInmateId));
        }
        if (inmateName != null && !inmateName.isBlank()) {
            specification = specification.and(EnrollmentSpecifications.inmateNameContains(inmateName));
        }
        if (internalProgramId != null) {
            specification = specification.and(EnrollmentSpecifications.programId(internalProgramId));
        }
        if (programName != null && !programName.isBlank()) {
            specification = specification.and(EnrollmentSpecifications.programNameContains(programName));
        }
        if (status != null && !status.isBlank()) {
            specification = specification.and(EnrollmentSpecifications.statusIs(normalizeStatus(status)));
        }
        return enrollmentRepository.findAll(specification, pageable).map(EnrollmentResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> history(UUID inmateId, Pageable pageable) {
        Inmate inmate = inmateRepository.findByPublicUuid(inmateId).orElse(null);
        if (inmate == null) {
            throw new InmateService.InmateNotFoundException();
        }
        return enrollmentRepository.findByInmate_Id(inmate.getId(), pageable)
                .map(EnrollmentResponse::from);
    }

    public Enrollment findActiveEnrollment(UUID inmateUuid, UUID programUuid) {
        Inmate inmate = inmateRepository.findByPublicUuid(inmateUuid)
                .orElseThrow(InmateService.InmateNotFoundException::new);
        RehabilitationProgram program = programService.findProgram(programUuid);
        return enrollmentRepository.findByInmate_IdAndProgram_IdAndStatus(
                        inmate.getId(), program.getId(), ENROLLED)
                .orElseThrow(EnrollmentNotFoundException::new);
    }

    public Long getInmateId(UUID inmateUuid) {
        return inmateRepository.findByPublicUuid(inmateUuid)
                .map(Inmate::getId)
                .orElse(-1L);
    }

    public static String normalizeStatus(String status) {
        String normalized = status.trim().toLowerCase();
        if (!Set.of(ENROLLED, COMPLETED, WITHDRAWN, SUSPENDED).contains(normalized)) {
            throw new IllegalArgumentException("Invalid enrollment status.");
        }
        return normalized;
    }
}
