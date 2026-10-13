package com.pms.api.visitor.repository;

import com.pms.api.visitor.entity.Visitor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VisitorRepository extends JpaRepository<Visitor, Long>, JpaSpecificationExecutor<Visitor> {

    boolean existsByIdNumber(String idNumber);

    Optional<Visitor> findByIdNumber(String idNumber);
}
