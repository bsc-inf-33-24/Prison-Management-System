package com.pms.api.user.service;

import com.pms.api.user.dto.CreateUserRequest;
import com.pms.api.user.dto.UpdateUserDetailsRequest;
import com.pms.api.user.dto.UserResponse;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import com.pms.api.prison.entity.Prison;
import com.pms.api.prison.repository.PrisonRepository;
import com.pms.api.common.audit.Audited;
import com.pms.api.common.pagination.PageResponse;
import com.pms.api.common.pagination.PageableValidator;
import com.pms.api.common.pagination.PaginationProperties;
import com.pms.api.notification.service.NotificationService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.pms.api.auth.service.PasswordPolicy passwordPolicy;
    private final PaginationProperties paginationProperties;
    private final NotificationService notificationService;
    private final PrisonRepository prisonRepository;
    private final NewAccountEmailService newAccountEmailService;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       com.pms.api.auth.service.PasswordPolicy passwordPolicy,
                       PaginationProperties paginationProperties,
                       NotificationService notificationService, PrisonRepository prisonRepository,
                       org.springframework.beans.factory.ObjectProvider<NewAccountEmailService> emailServiceProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.paginationProperties = paginationProperties;
        this.notificationService = notificationService;
        this.prisonRepository = prisonRepository;
        this.newAccountEmailService = emailServiceProvider.getIfAvailable();
    }

    @Audited
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String username = request.username().trim();
        passwordPolicy.validate(request.password());
        validateRole(request.role());
        Prison prison = resolvePrisonForRole(request.role(), request.prisonId());
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException();
        }
        String email = request.email();
        if (email != null && userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User(
                request.fullName().trim(),
                username,
                email,
                passwordEncoder.encode(request.password()),
                request.role().trim(),
                Set.of()
        );
        user.assignPrison(prison);
        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new UserIdentityAlreadyExistsException();
        }
        notificationService.notifyUserAndRoles(
                saved,
                "USER_ACCOUNT_CREATED",
                "User account created",
                "Your user account has been created.",
                saved.getPublicUuid(),
                Set.of("Super Admin"));
        if (newAccountEmailService != null) newAccountEmailService.sendPasswordChangeInstruction(saved);
        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(String status, String role, String search,
                                               UUID prisonId, Integer page, Integer size, List<String> sort) {
        Pageable pageable = PageableValidator.create(page, size, sort,
                Set.of("id", "fullName", "username", "role", "status", "createdAt"),
                Sort.by(Sort.Order.asc("id")), paginationProperties);
        Specification<User> specification = Specification.unrestricted();
        if (status != null && !status.isBlank()) {
            if (!User.ACTIVE.equalsIgnoreCase(status) && !User.INACTIVE.equalsIgnoreCase(status)) {
                throw new IllegalArgumentException("Invalid user status.");
            }
            specification = specification.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("status")), status.trim().toLowerCase(Locale.ROOT)));
        }
        if (role != null && !role.isBlank()) {
            validateRole(role);
            specification = specification.and((root, query, cb) -> cb.equal(root.get("role"), role.trim()));
        }
        if (prisonId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("prison").get("publicUuid"), prisonId));
        }
        if (search != null && !search.isBlank()) {
            String value = "%" + escapeLike(search.trim()).toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), value, '!'),
                    cb.like(cb.lower(root.get("username")), value, '!'),
                    cb.like(cb.lower(root.get("email")), value, '!')));
        }
        return PageResponse.from(userRepository.findAll(specification, pageable).map(UserResponse::from));
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private Prison resolvePrisonForRole(String role, java.util.UUID prisonId) {
        if ("Super Admin".equals(role.trim())) {
            if (prisonId != null) throw new PrisonAssignmentNotAllowedException();
            return null;
        }
        if (prisonId == null) throw new PrisonAssignmentRequiredException();
        Prison prison = prisonRepository.findByPublicUuid(prisonId).orElseThrow(PrisonNotFoundException::new);
        if (!prison.isActive()) throw new PrisonInactiveException();
        return prison;
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID userId) {
        return UserResponse.from(getUser(userId));
    }

    @Audited
    @Transactional
    public UserResponse deactivate(UUID userId) {
        User user = getUser(userId);
        boolean wasActive = user.isActive();
        user.deactivate();
        if (wasActive) {
            notificationService.notifyRoles(
                    "USER_ACCOUNT_DEACTIVATED",
                    "User account deactivated",
                    "A user account has been deactivated.",
                    user.getPublicUuid(),
                    Set.of("Super Admin"));
        }
        return UserResponse.from(user);
    }

    @Audited
    @Transactional
    public UserResponse reactivate(UUID userId) {
        User user = getUser(userId);
        boolean wasInactive = !user.isActive();
        user.reactivate();
        if (wasInactive) {
            notificationService.notifyUserAndRoles(
                    user,
                    "USER_ACCOUNT_REACTIVATED",
                    "User account reactivated",
                    "Your user account has been reactivated.",
                    user.getPublicUuid(),
                    Set.of("Super Admin"));
        }
        return UserResponse.from(user);
    }

    @Audited
    @Transactional
    public UserResponse updateDetails(UUID userId, UpdateUserDetailsRequest request) {
        User user = getUser(userId);
        String oldFullName = user.getFullName();
        String oldUsername = user.getUsername();
        String oldEmail = user.getEmail();
        String oldRole = user.getRole();
        java.util.UUID oldPrisonId = user.getPrison()==null?null:user.getPrison().getPublicUuid();
        Set<String> oldPermissions = new LinkedHashSet<>(user.getPermissions());
        String fullName = request.fullName() == null ? null : request.fullName().trim();
        String username = request.username() == null ? null : request.username().trim();
        String email = request.email();
        String role = request.role() == null ? null : request.role().trim();

        if (role != null) {
            validateRole(role);
        }
        String effectiveRole=role==null?user.getRole():role;
        if ("Super Admin".equals(effectiveRole)) {
            if (request.prisonId()!=null) throw new PrisonAssignmentNotAllowedException();
            user.assignPrison(null);
        } else if (request.prisonId()!=null) {
            user.assignPrison(resolvePrisonForRole(effectiveRole,request.prisonId()));
        } else if (user.getPrison()==null) {
            throw new PrisonAssignmentRequiredException();
        }

        if (fullName != null && fullName.isEmpty()) {
            throw new IllegalArgumentException("Full name must not be blank.");
        }
        if (username != null && username.isEmpty()) {
            throw new IllegalArgumentException("Username must not be blank.");
        }

        if (username != null && !username.equals(user.getUsername())
                && userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException();
        }
        if (email != null && !email.equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException();
        }

        user.updateDetails(fullName, username, email);
        if (role != null || request.permissions() != null) {
            user.replaceRoleAndPermissions(role == null ? user.getRole() : role,
                    request.permissions() == null
                            ? user.getPermissions()
                            : new LinkedHashSet<>(request.permissions()));
        }
        boolean changed = !java.util.Objects.equals(oldFullName, user.getFullName())
                || !java.util.Objects.equals(oldUsername, user.getUsername())
                || !java.util.Objects.equals(oldEmail, user.getEmail())
                || !java.util.Objects.equals(oldRole, user.getRole())
                || !java.util.Objects.equals(oldPrisonId,user.getPrison()==null?null:user.getPrison().getPublicUuid())
                || !oldPermissions.equals(user.getPermissions());
        if (changed) {
            notificationService.notifyUserAndRoles(
                    user,
                    "USER_ACCOUNT_UPDATED",
                    "User account updated",
                    "Your profile or account access details have changed.",
                    user.getPublicUuid(),
                    Set.of("Super Admin"));
        }
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(User user, String newPassword) {
        passwordPolicy.validate(newPassword);
        user.updatePasswordHash(passwordEncoder.encode(newPassword));
        user.resetLoginFailures();
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = UsernameNotFoundException.class)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password."));
        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + toAuthorityRole(user.getRole())));
        user.getPermissions().stream()
                .map(String::trim)
                .filter(permission -> !permission.isBlank())
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPasswordHash(), user.isActive(), true, true,
                !user.isLoginLocked(), authorities);
    }

    public User getUser(UUID userId) {
        return userRepository.findByPublicUuid(userId).orElseThrow(UserNotFoundException::new);
    }

    public static void validateRole(String role) {
        if (role == null || !User.ALLOWED_ROLES.contains(role.trim())) {
            throw new InvalidRoleException();
        }
    }

    public static String toAuthorityRole(String role) {
        return role.toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    public static class UserNotFoundException extends RuntimeException { }
    public static class UsernameAlreadyExistsException extends RuntimeException { }
    public static class EmailAlreadyExistsException extends RuntimeException { }
    public static class UserIdentityAlreadyExistsException extends RuntimeException { }
    public static class InvalidRoleException extends RuntimeException { }
    public static class PrisonNotFoundException extends RuntimeException { }
    public static class PrisonInactiveException extends RuntimeException { }
    public static class PrisonAssignmentRequiredException extends RuntimeException { }
    public static class PrisonAssignmentNotAllowedException extends RuntimeException { }
}
