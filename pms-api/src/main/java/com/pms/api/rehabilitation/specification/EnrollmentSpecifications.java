package com.pms.api.rehabilitation.specification;

import com.pms.api.rehabilitation.entity.Enrollment;
import org.springframework.data.jpa.domain.Specification;


public final class EnrollmentSpecifications {

    private EnrollmentSpecifications() {
    }

    public static Specification<Enrollment> inmateId(Long id) {
        return (root, query, cb) -> cb.equal(root.get("inmate").get("id"), id);
    }

    public static Specification<Enrollment> programId(Long id) {
        return (root, query, cb) -> cb.equal(root.get("program").get("id"), id);
    }

    public static Specification<Enrollment> statusIs(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Enrollment> inmateNameContains(String value) {
        String pattern = "%" + ProgramSpecifications.escape(value.trim().toLowerCase()) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("inmate").get("firstName")), pattern, '!'),
                cb.like(cb.lower(root.get("inmate").get("lastName")), pattern, '!')
        );
    }

    public static Specification<Enrollment> programNameContains(String value) {
        String pattern = "%" + ProgramSpecifications.escape(value.trim().toLowerCase()) + "%";
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("program").get("name")), pattern, '!');
    }
}
