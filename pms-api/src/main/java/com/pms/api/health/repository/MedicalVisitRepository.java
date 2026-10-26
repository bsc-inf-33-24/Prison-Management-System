package com.pms.api.health.repository;

import com.pms.api.health.entity.MedicalVisit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalVisitRepository extends JpaRepository<MedicalVisit, Long> {

    List<MedicalVisit> findAllByInmateIdOrderByVisitDateDescIdDesc(Long inmateId);
}
