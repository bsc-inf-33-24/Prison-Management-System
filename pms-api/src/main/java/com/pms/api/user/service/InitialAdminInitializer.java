package com.pms.api.user.service;

import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitialAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public InitialAdminInitializer(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   @Value("${INITIAL_ADMIN_USERNAME:}") String username,
                                   @Value("${INITIAL_ADMIN_PASSWORD:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (username.isBlank() || password.isBlank() || userRepository.existsByRole("Super Admin")) {
            return;
        }
        userRepository.save(new User(
                "Initial Super Admin",
                username.trim(),
                passwordEncoder.encode(password),
                "Super Admin",
                Set.of()
        ));
    }
}
