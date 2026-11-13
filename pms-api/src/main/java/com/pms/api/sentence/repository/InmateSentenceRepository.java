package com.pms.api.sentence.repository;
import com.pms.api.sentence.entity.InmateSentence;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InmateSentenceRepository extends JpaRepository<InmateSentence, Long> {
    List<InmateSentence> findByInmateIdOrderByStartDateDescIdDesc(Long inmateId);
    Optional<InmateSentence> findByPublicUuid(UUID publicUuid);
}
