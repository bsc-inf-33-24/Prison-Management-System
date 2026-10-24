package com.pms.api.rehabilitation.service;

import com.pms.api.common.audit.AuditService;
import com.pms.api.rehabilitation.dto.attendance.AttendanceResponse;
import com.pms.api.rehabilitation.dto.attendance.BulkRecordAttendanceRequest;
import com.pms.api.rehabilitation.dto.attendance.RecordAttendanceEntryRequest;
import com.pms.api.rehabilitation.dto.attendance.RecordAttendanceRequest;
import com.pms.api.rehabilitation.entity.AttendanceRecord;
import com.pms.api.rehabilitation.entity.Enrollment;
import com.pms.api.rehabilitation.exception.RehabilitationExceptions.AttendanceNotAllowedException;
import com.pms.api.rehabilitation.exception.RehabilitationExceptions.DuplicateAttendanceException;
import com.pms.api.rehabilitation.repository.AttendanceRecordRepository;
import com.pms.api.rehabilitation.specification.AttendanceSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        Enrollment enrollment = enrollmentService.findActiveEnrollment(request.inmateId(), request.programId());
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
    public List<AttendanceResponse> createBulk(BulkRecordAttendanceRequest request, String recordedBy) {
        Set<UUID> inmateIds = new HashSet<>();
        for (RecordAttendanceEntryRequest record : request.records()) {
            if (!inmateIds.add(record.inmateId())) {
                throw new DuplicateAttendanceException();
            }
        }

        return request.records().stream()
                .map(record -> create(new RecordAttendanceRequest(
                        record.inmateId(), request.programId(), request.sessionDate(),
                        record.attendanceStatus(), record.remarks()), recordedBy))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> list(Pageable pageable) {
        return list(null, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> list(UUID inmateId, String programName,
                                         String attendanceStatus, LocalDate sessionDateFrom,
                                         LocalDate sessionDateTo, Pageable pageable) {
        Specification<AttendanceRecord> specification =
                Specification.unrestricted();
        Long internalInmateId = inmateId == null ? null : enrollmentService.getInmateId(inmateId);
        if (internalInmateId != null) {
            specification = specification.and(AttendanceSpecifications.inmateId(internalInmateId));
        }
        if (programName != null && !programName.isBlank()) {
            specification = specification.and(AttendanceSpecifications.programNameContains(programName));
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
