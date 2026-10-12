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
        auditService.record(recordedBy, "ATTENDANCE_CREATED", "AttendanceRecord", saved.getId(), null,
                "enrollmentId=" + enrollment.getId() + ",present=" + saved.getPresent());
        return AttendanceResponse.from(saved);
    }

    @Transactional
    public AttendanceResponse update(Long attendanceId, UpdateAttendanceRequest request,
                                     String username) {
        AttendanceRecord attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(AttendanceNotFoundException::new);
        validateAttendanceAllowed(attendance.getEnrollment());
        String oldValue = attendance.getPresent();
        attendance.updateAttendance(toDatabaseValue(request.attendanceStatus()), request.remarks());
        auditService.record(username, "ATTENDANCE_UPDATED", "AttendanceRecord", attendance.getId(),
                "present=" + oldValue, "present=" + attendance.getPresent());
        return AttendanceResponse.from(attendance);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> list(Pageable pageable) {
        return list(null, null, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> list(Long enrollmentId, Long inmateId, Long programId,
                                         String attendanceStatus, LocalDate sessionDateFrom,
                                         LocalDate sessionDateTo, Pageable pageable) {
        Specification<AttendanceRecord> specification =
                Specification.where((Specification<AttendanceRecord>) null);
        if (enrollmentId != null) {
            specification = specification.and(AttendanceSpecifications.enrollmentId(enrollmentId));
        }
        if (inmateId != null) {
            specification = specification.and(AttendanceSpecifications.inmateId(inmateId));
        }
        if (programId != null) {
            specification = specification.and(AttendanceSpecifications.programId(programId));
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
    public Page<AttendanceResponse> history(Long enrollmentId, Pageable pageable) {
        enrollmentService.findEnrollment(enrollmentId);
        return attendanceRepository.findByEnrollment_IdOrderBySessionDateDescIdDesc(enrollmentId, pageable)
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
