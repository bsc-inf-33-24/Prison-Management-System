package com.pms.api.health.service;

import com.pms.api.common.audit.AuditService;
import com.pms.api.health.dto.CreateMedicalAppointmentRequest;
import com.pms.api.health.dto.MedicalAppointmentResponse;
import com.pms.api.health.entity.HealthRecord;
import com.pms.api.health.entity.MedicalAppointment;
import com.pms.api.health.repository.HealthRecordRepository;
import com.pms.api.health.repository.MedicalAppointmentRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class MedicalAppointmentService {

    private final MedicalAppointmentRepository appointmentRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final AuditService auditService;

    public MedicalAppointmentService(
            MedicalAppointmentRepository appointmentRepository,
            HealthRecordRepository healthRecordRepository,
            AuditService auditService) {
        this.appointmentRepository = appointmentRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.auditService = auditService;
    }

    public MedicalAppointmentResponse create(
            CreateMedicalAppointmentRequest request,
            String username) {

        HealthRecord healthRecord = healthRecordRepository
                .findByPublicUuid(request.healthRecordId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Health record was not found."));

        String appointmentType =
                request.appointmentType().trim().toLowerCase(Locale.ROOT);

        if (!appointmentType.equals("internal")
                && !appointmentType.equals("external")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment type must be internal or external.");
        }

        String status = request.status() == null
                || request.status().isBlank()
                ? "scheduled"
                : request.status().trim().toLowerCase(Locale.ROOT);

        if (!status.equals("scheduled")
                && !status.equals("completed")
                && !status.equals("cancelled")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Appointment status must be scheduled, completed, or cancelled.");
        }

        String externalFacility = request.externalFacility() == null
                ? null
                : request.externalFacility().trim();

        if (appointmentType.equals("external")
                && (externalFacility == null || externalFacility.isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "An external facility is required for external appointments.");
        }

        if (appointmentType.equals("internal")) {
            externalFacility = null;
        }

        MedicalAppointment appointment = new MedicalAppointment(
                healthRecord,
                request.appointmentDate(),
                request.purpose().trim(),
                appointmentType,
                externalFacility,
                status,
                username
        );

        MedicalAppointment saved = appointmentRepository.save(appointment);

        auditService.record(
                username,
                "MEDICAL_APPOINTMENT_CREATED",
                "MedicalAppointment",
                saved.getPublicUuid().toString(),
                null,
                "healthRecordId=" + healthRecord.getPublicUuid()
        );

        return MedicalAppointmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public MedicalAppointmentResponse get(UUID id) {
        MedicalAppointment appointment = appointmentRepository
                .findByPublicUuid(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Appointment was not found."));

        return MedicalAppointmentResponse.from(appointment);
    }

    @Transactional(readOnly = true)
    public Page<MedicalAppointmentResponse> list(
            String search, Pageable pageable) {

        Specification<MedicalAppointment> specification =
                (root, query, criteriaBuilder) ->
                        criteriaBuilder.conjunction();

        if (search != null && !search.isBlank()) {
            String pattern = "%"
                    + search.trim().toLowerCase(Locale.ROOT)
                    + "%";

            specification = specification.and(
                    (root, query, criteriaBuilder) ->
                            criteriaBuilder.or(
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("purpose")),
                                            pattern),
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("appointmentType")),
                                            pattern),
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("status")),
                                            pattern),
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("externalFacility")),
                                            pattern),
                                    criteriaBuilder.like(
                                            criteriaBuilder.lower(
                                                    root.get("scheduledBy")),
                                            pattern)
                            )
            );
        }

        return appointmentRepository.findAll(specification, pageable)
                .map(MedicalAppointmentResponse::from);
    }
}