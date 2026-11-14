package com.pms.api.inmate.controller;

import com.pms.api.inmate.service.InmatePhotoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inmates/{inmateId}/photo")
@SecurityRequirement(name = "bearerAuth")
public class InmatePhotoController {
    private final InmatePhotoService photos;
    public InmatePhotoController(InmatePhotoService photos) { this.photos = photos; }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'PRISON_OFFICER')")
    public ResponseEntity<byte[]> getPhoto(@PathVariable UUID inmateId) {
        InmatePhotoService.PhotoContent photo = photos.get(inmateId);
        MediaType mediaType = MediaType.parseMediaType(photo.contentType());
        return ResponseEntity.ok().contentType(mediaType).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(photo.bytes());
    }
}
