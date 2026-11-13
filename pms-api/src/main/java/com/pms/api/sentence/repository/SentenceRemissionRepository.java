package com.pms.api.sentence.repository;
import com.pms.api.sentence.entity.SentenceRemission;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface SentenceRemissionRepository extends JpaRepository<SentenceRemission, Long> {
    List<SentenceRemission> findBySentenceIdOrderByEffectiveDateAscIdAsc(Long sentenceId);
    @Query("select coalesce(sum(r.remissionPercentage), 0) from SentenceRemission r where r.sentence.id = :sentenceId")
    BigDecimal sumPercentageForSentence(@Param("sentenceId") Long sentenceId);
}
