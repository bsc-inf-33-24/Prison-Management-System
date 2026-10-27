package com.pms.api.health.repository;

import com.pms.api.health.entity.MedicalVisit;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalVisitRepository extends JpaRepository<MedicalVisit, Long> {

    @Query("""
            select visit from MedicalVisit visit
            where visit.inmate.id = :inmateId
              and (:name is null
                   or lower(concat(visit.inmate.firstName, ' ', visit.inmate.lastName))
                      like lower(concat('%', :name, '%')))
            order by visit.visitDate desc, visit.id desc
            """)
    List<MedicalVisit> findByInmateIdAndInmateNameOrderByVisitDateDescIdDesc(
            @Param("inmateId") Long inmateId, @Param("name") String name);
}
