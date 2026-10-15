package com.pms.api.rehabilitation.repository;

import com.pms.api.rehabilitation.entity.RehabilitationProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import java.util.UUID;

public interface RehabilitationProgramRepository
        extends JpaRepository<RehabilitationProgram, Long>,
                JpaSpecificationExecutor<RehabilitationProgram> {

    Optional<RehabilitationProgram> findByPublicUuid(UUID publicUuid);

    long countByIdAndStatus(Long programId, String status);
}
