package com.pms.api.roster.repository;

import com.pms.api.roster.entity.RosterShift;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RosterShiftRepository extends JpaRepository<RosterShift, Long> {

    List<RosterShift> findByRoster_IdOrderByStartAtAscIdAsc(Long rosterId);

    Optional<RosterShift> findByRoster_IdAndPublicUuid(Long rosterId, UUID publicUuid);

    @Query("""
            select case when count(s) > 0 then true else false end
            from RosterShift s
            where s.assignedOfficer.id = :officerId
              and s.startAt < :endAt
              and s.endAt > :startAt
              and (:excludeShiftId is null or s.id <> :excludeShiftId)
            """)
    boolean existsOverlappingShift(@Param("officerId") Long officerId,
                                   @Param("startAt") OffsetDateTime startAt,
                                   @Param("endAt") OffsetDateTime endAt,
                                   @Param("excludeShiftId") Long excludeShiftId);

    @Query("""
            select s from RosterShift s
            where s.assignedOfficer.id = :officerId
              and s.roster.status = com.pms.api.roster.entity.RosterStatus.PUBLISHED
              and s.startAt < :toExclusive
              and s.endAt > :fromInclusive
            order by s.startAt, s.id
            """)
    List<RosterShift> findPublishedForOfficerBetween(@Param("officerId") Long officerId,
                                                      @Param("fromInclusive") OffsetDateTime fromInclusive,
                                                      @Param("toExclusive") OffsetDateTime toExclusive);
}
