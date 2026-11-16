package com.pms.api.sentence.repository;

import com.pms.api.sentence.entity.SentenceExtensionRequest;
import com.pms.api.sentence.entity.SentenceExtensionStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SentenceExtensionRequestRepository extends JpaRepository<SentenceExtensionRequest, Long> {
    List<SentenceExtensionRequest> findBySentence_IdOrderByRequestedAtDescIdDesc(Long sentenceId);

    boolean existsBySentence_IdAndDecision(Long sentenceId, SentenceExtensionStatus decision);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from SentenceExtensionRequest request where request.publicUuid = :publicUuid")
    Optional<SentenceExtensionRequest> findByPublicUuidForUpdate(@Param("publicUuid") UUID publicUuid);
}
