package com.pms.api.cases.repository;

import com.pms.api.cases.entity.Crime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CrimeRepository extends JpaRepository<Crime, Long>, JpaSpecificationExecutor<Crime> {
}
