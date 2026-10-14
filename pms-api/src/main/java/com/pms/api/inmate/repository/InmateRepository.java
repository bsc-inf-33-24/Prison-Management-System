package com.pms.api.inmate.repository;

import com.pms.api.inmate.entity.Inmate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InmateRepository extends JpaRepository<Inmate, Long>, JpaSpecificationExecutor<Inmate> {

    @Query("""
            select i from Inmate i
            where lower(i.firstName) like lower(concat('%', :search, '%')) escape '!'
               or lower(i.lastName) like lower(concat('%', :search, '%')) escape '!'
               or lower(i.inmateNumber) like lower(concat('%', :search, '%')) escape '!'
            order by i.id
            """)
}
