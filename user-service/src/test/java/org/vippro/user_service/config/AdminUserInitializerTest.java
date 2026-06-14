package org.vippro.user_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.vippro.user_service.model.UserAccount;
import org.vippro.user_service.model.UserRole;
import org.vippro.user_service.repository.UserAccountRepository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserInitializerTest {

    @Test
    void createsConfiguredAdminWhenMissing() {
        UserAccountRepository repository =
                mock(UserAccountRepository.class);
        BCryptPasswordEncoder passwordEncoder =
                new BCryptPasswordEncoder();
        UserBootstrapProperties properties =
                new UserBootstrapProperties(
                        true,
                        "admin",
                        "admin@example.com",
                        "strong-password-123",
                        "System Admin"
                );
        when(repository.save(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserInitializer initializer = new AdminUserInitializer(
                properties,
                repository,
                passwordEncoder
        );
        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(repository).save(
                org.mockito.ArgumentMatchers.argThat(user ->
                        user.getRoles().contains(UserRole.ADMIN)
                                && passwordEncoder.matches(
                                properties.password(),
                                user.getPasswordHash()
                        )
                )
        );
    }
}
