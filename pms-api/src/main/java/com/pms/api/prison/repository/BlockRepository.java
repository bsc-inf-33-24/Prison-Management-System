package com.pms.api.prison.repository;

import com.pms.api.prison.entity.Block;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockRepository extends JpaRepository<Block, Long> {
    Optional<Block> findByPublicUuid(UUID id);
    Optional<Block> findByPublicUuidAndPrison_PublicUuid(UUID id, UUID prisonId);
    Optional<Block> findByPrison_IdAndNameIgnoreCase(Long prisonId, String name);
    boolean existsByPrison_IdAndNameIgnoreCase(Long prisonId, String name);
    List<Block> findByPrison_IdOrderByNameAsc(Long prisonId);
}
