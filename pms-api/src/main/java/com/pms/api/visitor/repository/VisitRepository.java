package com.pms.api.visitor.repository;

import com.pms.api.visitor.entity.Visit;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitRepository extends JpaRepository<Visit, Long>, JpaSpecificationExecutor<Visit> {

    Optional<Visit> findByPublicUuid(UUID publicUuid);

    Page<Visit> findByInmate_IdOrderByStartDateTimeDesc(Long inmateId, Pageable pageable);

    @Query("""
            select visit from Visit visit
            where visit.endDateTime >= :windowStart
                and visit.endDateTime < :windowEnd
                and visit.startDateTime <= :now
                and visit.endReminderSentAt is null
            order by visit.endDateTime
            """)
    List<Visit> findVisitsDueForEndReminder(@Param("windowStart") OffsetDateTime windowStart,
                                            @Param("windowEnd") OffsetDateTime windowEnd,
                                            @Param("now") OffsetDateTime now);

    @Modifying
    @Query("""
            update Visit visit
            set visit.endReminderSentAt = :sentAt
            where visit.id = :visitId
                and visit.endReminderSentAt is null
                and visit.endDateTime >= :windowStart
                and visit.endDateTime < :windowEnd
                and visit.startDateTime <= :now
            """)
    int claimEndReminder(@Param("visitId") Long visitId,
                         @Param("sentAt") Instant sentAt,
                         @Param("windowStart") OffsetDateTime windowStart,
                         @Param("windowEnd") OffsetDateTime windowEnd,
                         @Param("now") OffsetDateTime now);
}
