package com.pms.api.sentence.repository;
import com.pms.api.sentence.entity.InmateSentence;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface InmateSentenceRepository extends JpaRepository<InmateSentence, Long> {
    List<InmateSentence> findByInmateIdOrderByStartDateDescIdDesc(Long inmateId);
    Optional<InmateSentence> findByPublicUuid(UUID publicUuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sentence from InmateSentence sentence where sentence.publicUuid = :publicUuid")
    Optional<InmateSentence> findByPublicUuidForUpdate(@Param("publicUuid") UUID publicUuid);
}
