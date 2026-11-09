package com.pms.api.cell.repository;

import com.pms.api.cell.entity.CellAllocation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CellAllocationRepository extends JpaRepository<CellAllocation, Long> {

    Optional<CellAllocation> findByPublicUuid(UUID publicUuid);

    @Query("select count(a) from CellAllocation a where a.cell.id = :cellId and a.releasedAt is null")
    long countByCellId(@Param("cellId") Long cellId);

    @Query("select a.cell.id, count(a) from CellAllocation a where a.cell.id in :cellIds and a.releasedAt is null group by a.cell.id")
    List<Object[]> countByCellIds(@Param("cellIds") List<Long> cellIds);

    Optional<CellAllocation> findByInmate_IdAndReleasedAtIsNull(Long inmateId);

    List<CellAllocation> findByCell_IdAndReleasedAtIsNullOrderByAllocatedAtAsc(Long cellId);

    @Modifying
    @Query("update CellAllocation a set a.releasedAt = :releasedAt where a.inmate.id = :inmateId and a.releasedAt is null")
    int releaseByInmateId(@Param("inmateId") Long inmateId, @Param("releasedAt") java.time.Instant releasedAt);
}
