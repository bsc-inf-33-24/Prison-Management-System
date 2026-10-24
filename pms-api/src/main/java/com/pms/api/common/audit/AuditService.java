package com.pms.api.common.audit;

import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(
            String username,
            String action,
            String entityName,
            String publicRecordId,
            String oldValues,
            String newValues) {

        User user = username == null || username.isBlank()
                ? null
                : userRepository.findByUsername(username).orElse(null);

        auditLogRepository.save(new AuditLog(
                user, action, entityName, publicRecordId,
                oldValues, newValues));
    }

    @Transactional
    public void recordSecurityEvent(
            String username,
            String action,
            String endpoint,
            String reason) {

        String safeEndpoint = endpoint == null ? "" : endpoint;
        if (safeEndpoint.length() > 50) {
            safeEndpoint = safeEndpoint.substring(0, 50);
        }

        record(username, action, "Security", safeEndpoint, null, reason);
    }
}
