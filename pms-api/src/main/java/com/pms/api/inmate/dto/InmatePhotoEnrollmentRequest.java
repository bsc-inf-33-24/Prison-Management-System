package com.pms.api.inmate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

/** OpenAPI model for the multipart inmate enrollment request. */
@Schema(name = "InmatePhotoEnrollmentRequest", description = "Inmate enrollment fields and an optional photo.")
public record InmatePhotoEnrollmentRequest(
        @Schema(description = "Enrollment payload encoded as JSON.",
                implementation = InmateRequest.class,
                requiredMode = Schema.RequiredMode.REQUIRED)
        InmateRequest inmate,
        @Schema(description = "Optional JPEG or PNG inmate photo, up to 5 MB.",
                type = "string", format = "binary")
        MultipartFile photo) {
}
