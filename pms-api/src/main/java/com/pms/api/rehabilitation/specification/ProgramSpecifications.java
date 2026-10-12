package com.pms.api.rehabilitation.specification;

import com.pms.api.rehabilitation.entity.RehabilitationProgram;
import org.springframework.data.jpa.domain.Specification;

public final class ProgramSpecifications {

    private ProgramSpecifications() {
    }

    public static Specification<RehabilitationProgram> nameContains(String value) {
        return textContains("name", value);
    }

    public static Specification<RehabilitationProgram> instructorContains(String value) {
        return textContains("instructor", value);
    }

    public static Specification<RehabilitationProgram> scheduleContains(String value) {
        return textContains("schedule", value);
    }

    public static Specification<RehabilitationProgram> statusIs(String value) {
        return (root, query, cb) -> cb.equal(root.get("status"), value);
    }

    private static Specification<RehabilitationProgram> textContains(String field, String value) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get(field)),
                "%" + escape(value.trim().toLowerCase()) + "%",
                '!'
        );
    }

    public static String escape(String value) {
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
