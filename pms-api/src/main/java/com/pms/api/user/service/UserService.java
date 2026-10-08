package com.pms.api.user.service;

import com.pms.api.user.dto.CreateUserRequest;
import com.pms.api.user.dto.UpdateRoleRequest;
import com.pms.api.user.dto.UserResponse;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String username = request.username().trim();
        validateRole(request.role());
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException();
        }

        User user = new User(
                request.fullName().trim(),
                username,
                passwordEncoder.encode(request.password()),
                request.role().trim(),
                Set.of()
        );
        try {
            return UserResponse.from(userRepository.save(user));
        } catch (DataIntegrityViolationException exception) {
            throw new UsernameAlreadyExistsException();
        }
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(String search, String role, String status, Pageable pageable) {
        return userRepository.searchUsers(normalizeFilter(search), normalizeFilter(role), normalizeFilter(status), pageable)
                .map(UserResponse::from);
    }

    private static String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long userId) {
        return UserResponse.from(getUser(userId));
    }

    @Transactional
    public UserResponse deactivate(Long userId) {
        User user = getUser(userId);
        user.deactivate();
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse reactivate(Long userId) {
        User user = getUser(userId);
        user.reactivate();
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateRole(Long userId, UpdateRoleRequest request) {
        validateRole(request.role());
        User user = getUser(userId);
        user.replaceRoleAndPermissions(request.role().trim(),
                request.permissions() == null ? Set.of() : new LinkedHashSet<>(request.permissions()));
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(User user, String newPassword) {
        user.updatePasswordHash(passwordEncoder.encode(newPassword));
        user.resetLoginFailures();
    }

    @Override
    @Transactional(readOnly = true)
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

    public User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
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
    public static class InvalidRoleException extends RuntimeException { }
}
