package com.pms.api.health.dto;

import com.pms.api.inmate.entity.Inmate;
import java.util.List;
import java.util.UUID;

public record MedicalHistoryResponse(
        UUID inmateId,
        String inmateNumber,
        String inmateName,
        List<MedicalVisitResponse> visits
) {

    public static MedicalHistoryResponse from(Inmate inmate, List<MedicalVisitResponse> visits) {
        return new MedicalHistoryResponse(
                inmate.getPublicUuid(),
                inmate.getInmateNumber(),
                inmate.getFirstName() + " " + inmate.getLastName(),
                visits);
    }
}
