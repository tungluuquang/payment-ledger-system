package org.vippro.user_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.user_service.model.UserAccount;
import org.vippro.user_service.model.UserRole;
import org.vippro.user_service.model.UserStatus;
import org.vippro.user_service.repository.UserAccountRepository;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {

    private final UserBootstrapProperties properties;
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()
                || userRepository.existsByUsernameIgnoreCase(
                properties.username()
        )) {
            return;
        }

        if (properties.password() == null
                || properties.password().length() < 12) {
            throw new IllegalStateException(
                    "Bootstrap admin password must contain at least 12 characters"
            );
        }

        userRepository.save(
                UserAccount.builder()
                        .userId(UUID.randomUUID())
                        .username(
                                properties.username().trim().toLowerCase()
                        )
                        .email(properties.email().trim().toLowerCase())
                        .passwordHash(
                                passwordEncoder.encode(properties.password())
                        )
                        .fullName(properties.fullName().trim())
                        .status(UserStatus.ACTIVE)
                        .roles(Set.of(UserRole.ADMIN, UserRole.USER))
                        .build()
        );
    }
}
