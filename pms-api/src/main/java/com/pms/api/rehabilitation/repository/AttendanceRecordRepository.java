package com.pms.api.rehabilitation.repository;

import com.pms.api.rehabilitation.entity.AttendanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRecordRepository
        extends JpaRepository<AttendanceRecord, Long>,
                JpaSpecificationExecutor<AttendanceRecord> {

    Optional<AttendanceRecord> findByPublicUuid(UUID publicUuid);

    boolean existsByEnrollment_IdAndSessionDate(Long enrollmentId, java.time.LocalDate sessionDate);

}
