package com.pms.api.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_username", columnNames = "username")
)
public class User {

    public static final String ACTIVE = "active";
    public static final String INACTIVE = "inactive";

    public static final Set<String> ALLOWED_ROLES = Set.of(
            "Super Admin",
            "Records Officer",
            "Disciplinary Officer",
            "Case Officer",
            "Visitor Officer",
            "Rehabilitation Officer",
            "Medical Officer",
            "Duty Officer"
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "username", nullable = false, length = 16)
    private String username;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "status", nullable = false, length = 16)
    private String status = ACTIVE;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @ElementCollection
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission", nullable = false)
    private Set<String> permissions = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String fullName, String username, String passwordHash, String role, Set<String> permissions) {
        this.fullName = fullName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        replacePermissions(permissions);
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public Set<String> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isLoginLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    public void recordFailedLogin(int maximumAttempts, Instant lockUntil) {
        if (lockedUntil != null && !isLoginLocked()) {
            failedLoginAttempts = 0;
            lockedUntil = null;
        }
        failedLoginAttempts++;
        if (failedLoginAttempts >= maximumAttempts) {
            lockedUntil = lockUntil;
        }
    }

    public void resetLoginFailures() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void replaceRoleAndPermissions(String role, Set<String> permissions) {
        this.role = role;
        replacePermissions(permissions);
    }

    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    public void deactivate() {
        this.status = INACTIVE;
    }

    public void reactivate() {
        this.status = ACTIVE;
    }

    private void replacePermissions(Set<String> permissions) {
        this.permissions.clear();
        if (permissions != null) {
            this.permissions.addAll(permissions);
        }
    }

    @PrePersist
    private void setTimestampsOnCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void updateTimestamp() {
        updatedAt = Instant.now();
    }
}
