package com.pms.api.cases.repository;

import com.pms.api.cases.entity.Crime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrimeRepository extends JpaRepository<Crime, Long> {
}
