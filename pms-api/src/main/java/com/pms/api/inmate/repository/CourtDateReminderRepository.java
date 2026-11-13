package com.pms.api.inmate.repository;

import com.pms.api.inmate.entity.Inmate;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourtDateReminderRepository extends Repository<Inmate, Long> {
    @Query("select i from Inmate i where i.inmateType = com.pms.api.inmate.entity.InmateType.REMAND and i.expectedCourtDate = :date")
    List<Inmate> findRemandHearings(@Param("date") LocalDate date);

    @Modifying
    @Query(value = "INSERT INTO court_date_reminder_deliveries(inmate_id,court_date,days_before,channel,sent_at) VALUES (:inmateId,:courtDate,:daysBefore,:channel,:sentAt) ON CONFLICT (inmate_id,court_date,days_before,channel) DO NOTHING", nativeQuery = true)
    int claim(@Param("inmateId") Long inmateId, @Param("courtDate") LocalDate courtDate,
              @Param("daysBefore") int daysBefore, @Param("channel") String channel,
              @Param("sentAt") Instant sentAt);

    @Modifying
    @Query(value = "DELETE FROM court_date_reminder_deliveries WHERE inmate_id=:inmateId AND court_date=:courtDate AND days_before=:daysBefore AND channel=:channel", nativeQuery = true)
    int release(@Param("inmateId") Long inmateId, @Param("courtDate") LocalDate courtDate,
                @Param("daysBefore") int daysBefore, @Param("channel") String channel);
}
