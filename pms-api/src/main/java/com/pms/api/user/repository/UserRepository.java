package com.pms.api.user.repository;

import com.pms.api.user.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRole(String role);

    @Query("""
            select u from User u
            where (:search is null or :search = ''
                or lower(u.username) like lower(concat('%', :search, '%'))
                or lower(u.fullName) like lower(concat('%', :search, '%')))
              and (:role is null or u.role = :role)
              and (:status is null or u.status = :status)
            order by u.id asc
            """)
    Page<User> searchUsers(@Param("search") String search,
                           @Param("role") String role,
                           @Param("status") String status,
                           Pageable pageable);
}
