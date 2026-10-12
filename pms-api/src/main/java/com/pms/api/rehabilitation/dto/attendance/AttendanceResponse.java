package com.pms.api.rehabilitation.dto.attendance;

import com.pms.api.rehabilitation.entity.AttendanceRecord;

import java.time.LocalDate;

public record AttendanceResponse(
        Long id,
        Long enrollmentId,
        LocalDate sessionDate,
        String attendanceStatus,
        String remarks,
        String recordedBy
) {
    public static AttendanceResponse from(AttendanceRecord attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getEnrollment().getId(),
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
