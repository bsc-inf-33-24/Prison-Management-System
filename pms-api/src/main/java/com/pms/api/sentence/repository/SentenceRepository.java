package com.pms.api.sentence.repository;

import com.pms.api.sentence.entity.Sentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import java.util.UUID;

public interface SentenceRepository extends JpaRepository<Sentence, Long>, JpaSpecificationExecutor<Sentence> {
    Optional<Sentence> findByPublicUuid(UUID publicUuid);
}
