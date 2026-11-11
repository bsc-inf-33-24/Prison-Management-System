package com.pms.api.roster.repository;

import com.pms.api.roster.entity.Roster;
import com.pms.api.roster.entity.RosterStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RosterRepository extends JpaRepository<Roster, Long> {

    Optional<Roster> findByPublicUuid(UUID publicUuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Roster r where r.publicUuid = :publicUuid")
    Optional<Roster> findByPublicUuidForUpdate(@Param("publicUuid") UUID publicUuid);

    @Query("""
            select r from Roster r
            where (:prisonUuid is null or r.prison.publicUuid = :prisonUuid)
              and (:fromDate is null or r.periodEnd >= :fromDate)
              and (:toDate is null or r.periodStart <= :toDate)
              and (:status is null or r.status = :status)
            order by r.periodStart, r.id
            """)
    List<Roster> search(@Param("prisonUuid") UUID prisonUuid,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("status") RosterStatus status);
}
