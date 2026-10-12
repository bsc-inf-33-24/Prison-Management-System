package com.pms.api.cell.repository;

import com.pms.api.cell.entity.CellAllocation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CellAllocationRepository extends JpaRepository<CellAllocation, Long> {

    @Query("select count(a) from CellAllocation a where a.cell.id = :cellId")
    long countByCellId(@Param("cellId") Long cellId);

    Optional<CellAllocation> findByInmateId(Long inmateId);

    List<CellAllocation> findByCell_IdOrderByAllocatedAtAsc(Long cellId);

    @Modifying
    @Query("delete from CellAllocation a where a.inmate.id = :inmateId")
    int deleteByInmateId(@Param("inmateId") Long inmateId);
}
