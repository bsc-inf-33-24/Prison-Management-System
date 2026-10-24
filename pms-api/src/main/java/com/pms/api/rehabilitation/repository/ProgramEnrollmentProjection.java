package com.pms.api.rehabilitation.repository;

import java.util.UUID;

public interface ProgramEnrollmentProjection {
    Long getProgramId();

    UUID getInmateId();

    String getInmateName();
}
