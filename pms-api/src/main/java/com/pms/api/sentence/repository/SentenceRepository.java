package com.pms.api.sentence.repository;

import com.pms.api.sentence.entity.Sentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SentenceRepository extends JpaRepository<Sentence, Long>, JpaSpecificationExecutor<Sentence> {
}
