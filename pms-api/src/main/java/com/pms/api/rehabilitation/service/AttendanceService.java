package com.pms.api.rehabilitation.service;

import com.pms.api.common.audit.AuditService;
import com.pms.api.rehabilitation.dto.attendance.AttendanceResponse;
import com.pms.api.rehabilitation.dto.attendance.RecordAttendanceRequest;
import com.pms.api.rehabilitation.dto.attendance.UpdateAttendanceRequest;
import com.pms.api.rehabilitation.entity.AttendanceRecord;
import com.pms.api.rehabilitation.entity.Enrollment;
import com.pms.api.rehabilitation.exception.RehabilitationExceptions.AttendanceNotAllowedException;
import com.pms.api.rehabilitation.exception.RehabilitationExceptions.AttendanceNotFoundException;
import com.pms.api.rehabilitation.exception.RehabilitationExceptions.DuplicateAttendanceException;
import com.pms.api.rehabilitation.repository.AttendanceRecordRepository;
import com.pms.api.rehabilitation.specification.AttendanceSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import java.time.LocalDate;

@Service
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final EnrollmentService enrollmentService;
    private final AuditService auditService;

    public AttendanceService(AttendanceRecordRepository attendanceRepository,
                             EnrollmentService enrollmentService,
                             AuditService auditService) {
        this.attendanceRepository = attendanceRepository;
        this.enrollmentService = enrollmentService;
        this.auditService = auditService;
    }

    @Transactional
    public AttendanceResponse create(RecordAttendanceRequest request, String recordedBy) {
        Enrollment enrollment = enrollmentService.findEnrollment(request.enrollmentId());
        validateAttendanceDate(enrollment, request.sessionDate());
        validateAttendanceAllowed(enrollment);

        if (attendanceRepository.existsByEnrollment_IdAndSessionDate(
                enrollment.getId(), request.sessionDate())) {
            throw new DuplicateAttendanceException();
        }

        AttendanceRecord attendance = new AttendanceRecord(
                enrollment,
                request.sessionDate(),
                toDatabaseValue(request.attendanceStatus()),
                request.remarks(),
                recordedBy
        );
        AttendanceRecord saved = attendanceRepository.save(attendance);
        auditService.record(recordedBy, "ATTENDANCE_CREATED", "AttendanceRecord", saved.getPublicUuid().toString(), null,
                "enrollmentId=" + enrollment.getPublicUuid() + ",present=" + saved.getPresent());
        return AttendanceResponse.from(saved);
    }

    @Transactional
    public AttendanceResponse update(UUID attendanceId, UpdateAttendanceRequest request,
                                     String username) {
        AttendanceRecord attendance = attendanceRepository.findByPublicUuid(attendanceId)
                .orElseThrow(AttendanceNotFoundException::new);
        validateAttendanceAllowed(attendance.getEnrollment());
        String oldValue = attendance.getPresent();
        attendance.updateAttendance(toDatabaseValue(request.attendanceStatus()), request.remarks());
        auditService.record(username, "ATTENDANCE_UPDATED", "AttendanceRecord", attendance.getPublicUuid().toString(),
                "present=" + oldValue, "present=" + attendance.getPresent());
        return AttendanceResponse.from(attendance);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> list(Pageable pageable) {
        return list(null, null, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> list(UUID enrollmentId, UUID inmateId, UUID programId,
                                         String attendanceStatus, LocalDate sessionDateFrom,
                                         LocalDate sessionDateTo, Pageable pageable) {
        Specification<AttendanceRecord> specification =
                Specification.where((Specification<AttendanceRecord>) null);
        Long internalEnrollmentId = enrollmentId == null ? null : enrollmentService.findEnrollment(enrollmentId).getId();
        Long internalInmateId = inmateId == null ? null : enrollmentService.getInmateId(inmateId);
        Long internalProgramId = programId == null ? null : enrollmentService.getProgramId(programId);
        if (internalEnrollmentId != null) {
            specification = specification.and(AttendanceSpecifications.enrollmentId(internalEnrollmentId));
        }
        if (internalInmateId != null) {
            specification = specification.and(AttendanceSpecifications.inmateId(internalInmateId));
        }
        if (internalProgramId != null) {
            specification = specification.and(AttendanceSpecifications.programId(internalProgramId));
        }
        if (attendanceStatus != null && !attendanceStatus.isBlank()) {
            specification = specification.and(AttendanceSpecifications.statusIs(attendanceStatus));
        }
        if (sessionDateFrom != null) {
            specification = specification.and(AttendanceSpecifications.sessionDateFrom(sessionDateFrom));
        }
        if (sessionDateTo != null) {
            specification = specification.and(AttendanceSpecifications.sessionDateTo(sessionDateTo));
        }
        return attendanceRepository.findAll(specification, pageable).map(AttendanceResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> history(UUID enrollmentId, Pageable pageable) {
        enrollmentService.findEnrollment(enrollmentId);
        return attendanceRepository.findByEnrollment_IdOrderBySessionDateDescIdDesc(
                enrollmentService.findEnrollment(enrollmentId).getId(), pageable)
                .map(AttendanceResponse::from);
    }

    private static void validateAttendanceDate(Enrollment enrollment, LocalDate sessionDate) {
        if (enrollment.getEnrollmentDate() == null
                || sessionDate.isBefore(enrollment.getEnrollmentDate())) {
            throw new AttendanceNotAllowedException();
        }
    }

    private static void validateAttendanceAllowed(Enrollment enrollment) {
        if (!EnrollmentService.ENROLLED.equals(enrollment.getStatus())) {
            throw new AttendanceNotAllowedException();
        }
    }

    private static String toDatabaseValue(String attendanceStatus) {
        return switch (attendanceStatus.trim().toLowerCase()) {
            case "present" -> "Y";
            case "absent" -> "N";
            case "excused" -> throw new UnsupportedOperationException(
                    "The current database schema cannot store excused attendance.");
            default -> throw new IllegalArgumentException("Invalid attendance status.");
        };
    }
}
