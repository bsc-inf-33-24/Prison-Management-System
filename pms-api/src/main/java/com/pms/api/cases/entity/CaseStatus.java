package com.pms.api.cases.entity;

public enum CaseStatus {
    ONGOING,
    CLOSED,
    APPEALED;

    public static CaseStatus from(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        for (CaseStatus status : values()) {
            if (status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        return null;
    }
}
