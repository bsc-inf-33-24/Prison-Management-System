package com.pms.api.cell.repository;

import com.pms.api.cell.entity.Cell;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CellRepository extends JpaRepository<Cell, Long>, JpaSpecificationExecutor<Cell> {

    List<Cell> findByBlockIsNullOrderByBlockNameAsc();

    @Query("select c from Cell c join fetch c.block where c.block.id in :blockIds order by c.block.name asc, c.cellNumber asc")
    List<Cell> findAllByBlockIds(@Param("blockIds") List<Long> blockIds);

    Optional<Cell> findByPublicUuid(UUID publicUuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cell c where c.publicUuid = :publicUuid")
    Optional<Cell> findByPublicUuidForUpdate(@Param("publicUuid") UUID publicUuid);

}
