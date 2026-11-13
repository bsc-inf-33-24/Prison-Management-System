package com.pms.api.inmate.repository;

import com.pms.api.inmate.entity.InmatePhoto;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InmatePhotoRepository extends JpaRepository<InmatePhoto, Long> {
    Optional<InmatePhoto> findByInmateId(Long inmateId);
}
