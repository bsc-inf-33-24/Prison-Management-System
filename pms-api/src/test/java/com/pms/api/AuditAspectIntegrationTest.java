package com.pms.api;

import com.pms.api.common.audit.AuditLogRepository;
import com.pms.api.user.dto.CreateUserRequest;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import com.pms.api.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AuditAspectIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditedUserManagementActionsWriteAuditRecords() {
        String actorUsername = "audit-admin-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User actor = userRepository.saveAndFlush(new User(
                "Audit Admin",
                actorUsername,
                "audit-admin@example.test",
                "Password123!",
                "Super Admin",
                Set.of()
        ));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        actor.getUsername(),
                        "Password123!",
                        List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                )
        );

        String targetUsername = "audit-target-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        userService.create(new CreateUserRequest(
                "Audit Target",
                targetUsername,
                "audit-target@example.test",
                "StrongPass123!",
                "Records Officer"
        ));

        assertThat(auditLogRepository.findAll())
                .anySatisfy(log -> {
                    assertThat(log.getUser()).isNotNull();
                    assertThat(log.getUser().getUsername()).isEqualTo(actorUsername);
                    assertThat(log.getActionType()).isEqualTo("CREATE");
                    assertThat(log.getEntityName()).isEqualTo("UserService");
                });
    }
}
