package com.pms.api.sentence.repository;

import com.pms.api.sentence.entity.Sentence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SentenceRepository extends JpaRepository<Sentence, Long> {

    List<Sentence> findAllByOrderByIdAsc();

    List<Sentence> findByInmateIdOrderByIdAsc(Long inmateId);

    List<Sentence> findByCaseEntityIdOrderByIdAsc(Long caseId);
}
