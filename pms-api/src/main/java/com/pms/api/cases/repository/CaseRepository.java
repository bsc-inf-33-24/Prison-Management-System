package com.pms.api.cases.repository;

import com.pms.api.cases.entity.Case;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseRepository extends JpaRepository<Case, Long> {
    List<Case> findAllByOrderByCaseStartDateDesc();
    List<Case> findByInmateIdOrderByCaseStartDateDesc(Long inmateId);
}
