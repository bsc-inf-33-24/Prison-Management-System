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
import com.pms.api.security.PrisonAccessService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final PrisonAccessService prisonAccess;

    public RehabilitationProgramService(RehabilitationProgramRepository programRepository,
                                        EnrollmentRepository enrollmentRepository,
                                        AuditService auditService, UserRepository userRepository, PrisonAccessService prisonAccess) {
        this.programRepository = programRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.prisonAccess=prisonAccess;
    }

    @Transactional
    public ProgramResponse create(CreateProgramRequest request, String username) {
        User actor = userRepository.findByUsername(username).orElseThrow();
        if ("Program Facilitator".equals(actor.getRole()) && request.instructorUserId() != null && !request.instructorUserId().equals(actor.getPublicUuid())) {
            throw new org.springframework.security.access.AccessDeniedException("Program facilitator can only assign themselves as the instructor.");
        }
        RehabilitationProgram program = new RehabilitationProgram(
                request.name().trim(),
                request.description(),
                "",
                request.schedule(),
                ACTIVE
        );
        if (request.instructorUserId() != null) {
            User instructor = validInstructor(request.instructorUserId());
            program.assignInstructorUser(instructor);
            program.updateDetails(program.getName(), program.getDescription(), instructor.getFullName(), program.getSchedule());
        }
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
        return list(name, instructor, status, schedule, pageable, null);
    }

    @Transactional(readOnly = true)
    public Page<ProgramResponse> list(String name, String instructor, String status,
                                      String schedule, Pageable pageable, String username) {
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
        if (username != null) {
            User user = userRepository.findByUsername(username).orElseThrow();
            if ("Instructor".equals(user.getRole()) || "Program Facilitator".equals(user.getRole())) {
                specification = specification.and((root, query, cb) -> cb.equal(root.get("instructorUser").get("id"), user.getId()));
            }
        }
        Page<RehabilitationProgram> programs = programRepository.findAll(specification, pageable);
        List<Long> programIds = programs.getContent().stream().map(RehabilitationProgram::getId).toList();
        UUID assignedPrison=prisonAccess.effectivePrison(null);
        Map<Long, List<EnrolledInmateResponse>> participantsByProgram = programIds.isEmpty()
                ? Map.of()
                : (assignedPrison==null
                    ? enrollmentRepository.findEnrolledParticipantsByProgramIds(programIds)
                    : enrollmentRepository.findEnrolledParticipantsByProgramIdsAndPrison(programIds,assignedPrison)).stream()
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
    public ProgramDetailResponse getDetails(UUID programId, String username) {
        RehabilitationProgram program = findProgram(programId);
        User user = userRepository.findByUsername(username).orElseThrow();
        if (("Instructor".equals(user.getRole()) || "Program Facilitator".equals(user.getRole()))
                && (program.getInstructorUser() == null || !program.getInstructorUser().getId().equals(user.getId()))) {
            throw new org.springframework.security.access.AccessDeniedException("Program is not assigned to this instructor.");
        }
        return ProgramDetailResponse.from(program,
                enrollmentRepository.countByProgram_IdAndStatusIn(program.getId(), java.util.List.of("enrolled", "suspended")),
                enrollmentRepository.countByProgram_IdAndStatus(program.getId(), "completed"));
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

    private User validInstructor(UUID id) {
        User user = userRepository.findByPublicUuid(id).orElseThrow(() -> new IllegalArgumentException("Instructor user was not found."));
        if (!user.isActive() || !"Instructor".equals(user.getRole())) throw new IllegalArgumentException("Selected user must be an active Instructor.");
        return user;
    }

    @Transactional
    public ProgramResponse update(UUID programId, UpdateProgramRequest request, String username) {
        RehabilitationProgram program = findProgram(programId);
        User actor = userRepository.findByUsername(username).orElseThrow();
        if (("Instructor".equals(actor.getRole()) || "Program Facilitator".equals(actor.getRole()))
                && (program.getInstructorUser() == null || !program.getInstructorUser().getId().equals(actor.getId()))) {
            throw new org.springframework.security.access.AccessDeniedException("Program is not assigned to this instructor.");
        }
        if (request.instructorUserId() != null) {
            User instructor = validInstructor(request.instructorUserId());
            if ("Program Facilitator".equals(actor.getRole()) && !instructor.getId().equals(actor.getId())) {
                throw new org.springframework.security.access.AccessDeniedException("Program facilitator can only assign themselves as the instructor.");
            }
            program.assignInstructorUser(instructor);
            program.updateDetails(program.getName(), program.getDescription(), instructor.getFullName(), program.getSchedule());
        }
        String oldValues = "name=" + program.getName() + ",instructor=" + program.getInstructor();
        program.updateDetails(
                request.name().trim(),
                request.description(),
                program.getInstructorUser() == null ? program.getInstructor() : program.getInstructorUser().getFullName(),
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
