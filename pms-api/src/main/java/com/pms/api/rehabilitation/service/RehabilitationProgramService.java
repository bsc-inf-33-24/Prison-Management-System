package com.pms.api.rehabilitation.service;

import com.pms.api.common.audit.AuditService;
import com.pms.api.rehabilitation.dto.program.CreateProgramRequest;
import com.pms.api.rehabilitation.dto.program.ProgramDetailResponse;
import com.pms.api.rehabilitation.dto.program.EnrolledInmateResponse;
import com.pms.api.rehabilitation.dto.program.ProgramResponse;
import com.pms.api.rehabilitation.dto.program.UpdateProgramRequest;
import com.pms.api.rehabilitation.entity.RehabilitationProgram;
import com.pms.api.rehabilitation.exception.RehabilitationExceptions.ProgramNotFoundException;
import com.pms.api.rehabilitation.repository.EnrollmentRepository;
import com.pms.api.rehabilitation.repository.ProgramEnrollmentProjection;
import com.pms.api.rehabilitation.repository.RehabilitationProgramRepository;
import com.pms.api.rehabilitation.specification.ProgramSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class RehabilitationProgramService {

    public static final String ACTIVE = "active";
    public static final String INACTIVE = "inactive";
    public static final String COMPLETED = "completed";

    private final RehabilitationProgramRepository programRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AuditService auditService;

    public RehabilitationProgramService(RehabilitationProgramRepository programRepository,
                                        EnrollmentRepository enrollmentRepository,
                                        AuditService auditService) {
        this.programRepository = programRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ProgramResponse create(CreateProgramRequest request, String username) {
        RehabilitationProgram program = new RehabilitationProgram(
                request.name().trim(),
                request.description(),
                request.instructor().trim(),
                request.schedule(),
                ACTIVE
        );
        RehabilitationProgram saved = programRepository.save(program);
        auditService.record(username, "PROGRAM_CREATED", "RehabProgram", saved.getPublicUuid().toString(), null,
                "name=" + saved.getName() + ",status=" + saved.getStatus());
        return ProgramResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ProgramResponse> list(Pageable pageable) {
        return list(null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ProgramResponse> list(String name, String instructor, String status,
                                      String schedule, Pageable pageable) {
        Specification<RehabilitationProgram> specification =
                Specification.unrestricted();
        if (name != null && !name.isBlank()) {
            specification = specification.and(ProgramSpecifications.nameContains(name));
        }
        if (instructor != null && !instructor.isBlank()) {
            specification = specification.and(ProgramSpecifications.instructorContains(instructor));
        }
        if (status != null && !status.isBlank()) {
            specification = specification.and(ProgramSpecifications.statusIs(normalizeStatus(status)));
        }
        if (schedule != null && !schedule.isBlank()) {
            specification = specification.and(ProgramSpecifications.scheduleContains(schedule));
        }
        Page<RehabilitationProgram> programs = programRepository.findAll(specification, pageable);
        List<Long> programIds = programs.getContent().stream().map(RehabilitationProgram::getId).toList();
        Map<Long, List<EnrolledInmateResponse>> participantsByProgram = programIds.isEmpty()
                ? Map.of()
                : enrollmentRepository.findEnrolledParticipantsByProgramIds(programIds).stream()
                        .collect(Collectors.groupingBy(
                                ProgramEnrollmentProjection::getProgramId,
                                Collectors.mapping(participant -> new EnrolledInmateResponse(
                                                participant.getInmateId(), participant.getInmateName()),
                                        Collectors.toList())));
        return programs.map(program -> ProgramResponse.from(program,
                participantsByProgram.getOrDefault(program.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public ProgramResponse get(UUID programId) {
        return ProgramResponse.from(findProgram(programId));
    }

    @Transactional(readOnly = true)
    public ProgramDetailResponse getDetails(UUID programId) {
        RehabilitationProgram program = findProgram(programId);
        return ProgramDetailResponse.from(
                program,
                enrollmentRepository.countByProgram_IdAndStatusIn(
                        program.getId(), java.util.List.of("enrolled", "suspended")),
                enrollmentRepository.countByProgram_IdAndStatus(program.getId(), "completed")
        );
    }

    @Transactional
    public ProgramResponse update(UUID programId, UpdateProgramRequest request, String username) {
        RehabilitationProgram program = findProgram(programId);
        String oldValues = "name=" + program.getName() + ",instructor=" + program.getInstructor();
        program.updateDetails(
                request.name().trim(),
                request.description(),
                request.instructor().trim(),
                request.schedule()
        );
        auditService.record(username, "PROGRAM_UPDATED", "RehabProgram", program.getPublicUuid().toString(), oldValues,
                "name=" + program.getName() + ",instructor=" + program.getInstructor());
        return ProgramResponse.from(program);
    }

    public RehabilitationProgram findProgram(UUID programId) {
        return programRepository.findByPublicUuid(programId)
                .orElseThrow(ProgramNotFoundException::new);
    }

    public static String normalizeStatus(String status) {
        String normalized = status.trim().toLowerCase();
        if (!ACTIVE.equals(normalized) && !INACTIVE.equals(normalized) && !COMPLETED.equals(normalized)) {
            throw new IllegalArgumentException("Invalid program status.");
        }
        return normalized;
    }
}
