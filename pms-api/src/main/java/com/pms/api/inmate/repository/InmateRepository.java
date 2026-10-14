package com.pms.api.inmate.repository;

import com.pms.api.inmate.entity.Inmate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InmateRepository extends JpaRepository<Inmate, Long>, JpaSpecificationExecutor<Inmate> {
}
