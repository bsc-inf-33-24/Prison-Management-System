package com.pms.api.common.audit;

import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public void record(String username, String action, String entityName,
                       String publicRecordId, String oldValues, String newValues) {
        User user = userRepository.findByUsername(username).orElse(null);
        auditLogRepository.save(new AuditLog(
                user, action, entityName, publicRecordId, oldValues, newValues));
    }
}
