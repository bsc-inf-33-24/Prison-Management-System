package com.pms.api.rehabilitation.specification;

import com.pms.api.rehabilitation.entity.AttendanceRecord;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class AttendanceSpecifications {

    private AttendanceSpecifications() {
    }

    public static Specification<AttendanceRecord> inmateId(Long id) {
        return (root, query, cb) -> cb.equal(
                root.get("enrollment").get("inmate").get("id"), id);
    }

    public static Specification<AttendanceRecord> programNameContains(String name) {
        String pattern = "%" + ProgramSpecifications.escape(name.trim().toLowerCase()) + "%";
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("enrollment").get("program").get("name")), pattern, '!');
    }

    public static Specification<AttendanceRecord> statusIs(String status) {
        String databaseValue = switch (status.trim().toLowerCase()) {
            case "present" -> "Y";
            case "absent" -> "N";
            default -> status.trim();
        };
        return (root, query, cb) -> cb.equal(root.get("present"), databaseValue);
    }

    public static Specification<AttendanceRecord> sessionDateFrom(LocalDate date) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("sessionDate"), date);
    }

    public static Specification<AttendanceRecord> sessionDateTo(LocalDate date) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("sessionDate"), date);
    }
}
