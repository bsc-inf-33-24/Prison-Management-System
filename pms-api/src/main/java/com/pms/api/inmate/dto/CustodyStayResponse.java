package com.pms.api.inmate.dto;

import com.pms.api.inmate.entity.CustodyStay;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustodyStayResponse(UUID id, UUID inmateId, UUID prisonId, String prisonName,
                                  LocalDate admissionDate, Instant endedAt) {
    public static CustodyStayResponse from(CustodyStay stay) {
        return new CustodyStayResponse(stay.getPublicUuid(), stay.getInmate().getPublicUuid(),
                stay.getPrison().getPublicUuid(), stay.getPrison().getName(), stay.getAdmissionDate(), stay.getEndedAt());
    }
}
