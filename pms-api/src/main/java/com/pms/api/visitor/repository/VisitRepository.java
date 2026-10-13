package com.pms.api.visitor.repository;

import com.pms.api.visitor.entity.Visit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VisitRepository extends JpaRepository<Visit, Long>, JpaSpecificationExecutor<Visit> {

    Page<Visit> findByInmate_IdOrderByVisitDateTimeDesc(Long inmateId, Pageable pageable);
}
