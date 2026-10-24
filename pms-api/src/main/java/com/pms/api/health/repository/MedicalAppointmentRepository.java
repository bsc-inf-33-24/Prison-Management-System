package com.pms.api.health.repository;

import com.pms.api.health.entity.MedicalAppointment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MedicalAppointmentRepository
        extends JpaRepository<MedicalAppointment, Long>,
                JpaSpecificationExecutor<MedicalAppointment> {

    Optional<MedicalAppointment> findByPublicUuid(UUID publicUuid);
}