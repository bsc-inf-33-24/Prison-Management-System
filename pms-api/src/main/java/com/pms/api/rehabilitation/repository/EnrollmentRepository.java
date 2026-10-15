package com.pms.api.rehabilitation.repository;

import com.pms.api.rehabilitation.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository
        extends JpaRepository<Enrollment, Long>,
                JpaSpecificationExecutor<Enrollment> {

    Optional<Enrollment> findByPublicUuid(UUID publicUuid);

    boolean existsByInmate_IdAndProgram_IdAndStatusIn(
            Long inmateId, Long programId, Collection<String> statuses);

    long countByProgram_IdAndStatus(Long programId, String status);

    long countByProgram_IdAndStatusIn(Long programId, Collection<String> statuses);

    List<Enrollment> findByInmate_IdOrderByEnrollmentDateDescIdDesc(Long inmateId);

    Page<Enrollment> findByInmate_Id(Long inmateId, Pageable pageable);
}
