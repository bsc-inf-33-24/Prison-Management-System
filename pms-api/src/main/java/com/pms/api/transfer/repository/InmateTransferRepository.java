package com.pms.api.transfer.repository;

import com.pms.api.transfer.entity.InmateTransfer;
import com.pms.api.transfer.entity.TransferStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InmateTransferRepository extends JpaRepository<InmateTransfer, Long>, JpaSpecificationExecutor<InmateTransfer> {
    Optional<InmateTransfer> findByPublicUuid(UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from InmateTransfer t where t.publicUuid = :id")
    Optional<InmateTransfer> findByPublicUuidForUpdate(@Param("id") UUID id);
    boolean existsByInmate_IdAndStatusIn(Long inmateId, Collection<TransferStatus> statuses);
}
