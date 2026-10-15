package com.pms.api.user.repository;

import com.pms.api.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByPublicUuid(UUID publicUuid);

    boolean existsByUsername(String username);

    boolean existsByRole(String role);

}
