package com.pms.api.rehabilitation.dto.attendance;

import com.pms.api.rehabilitation.entity.AttendanceRecord;

import java.time.LocalDate;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        UUID enrollmentId,
        LocalDate sessionDate,
        String attendanceStatus,
        String remarks,
        String recordedBy
) {
    public static AttendanceResponse from(AttendanceRecord attendance) {
        return new AttendanceResponse(
                attendance.getPublicUuid(),
                attendance.getEnrollment().getPublicUuid(),
                attendance.getSessionDate(),
                fromDatabaseValue(attendance.getPresent()),
                attendance.getRemarks(),
                attendance.getRecordedBy()
        );
    }

    private static String fromDatabaseValue(String value) {
        return switch (value) {
            case "Y" -> "present";
            case "N" -> "absent";
            default -> value;
        };
    }
}
