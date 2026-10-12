package com.pms.api.rehabilitation.repository;

import com.pms.api.rehabilitation.entity.RehabilitationProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RehabilitationProgramRepository
        extends JpaRepository<RehabilitationProgram, Long>,
                JpaSpecificationExecutor<RehabilitationProgram> {

    long countByIdAndStatus(Long programId, String status);
}
