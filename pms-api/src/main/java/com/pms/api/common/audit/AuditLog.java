package com.pms.api.common.audit;

import com.pms.api.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "auditlog")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "entity_name")
    private String entityName;

    @Column(name = "record_id")
    private String recordId;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    @Column(name = "timestamp")
    private Instant timestamp;

    protected AuditLog() {
    }

    public AuditLog(User user, String actionType, String entityName, String recordId,
                    String oldValues, String newValues) {
        this.user = user;
        this.actionType = actionType;
        this.entityName = entityName;
        this.recordId = recordId;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.timestamp = Instant.now();
    }
}
