package org.vippro.user_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.vippro.user_service.dto.ChangePasswordRequest;
import org.vippro.user_service.dto.CreateUserRequest;
import org.vippro.user_service.exception.ConflictException;
import org.vippro.user_service.model.UserAccount;
import org.vippro.user_service.model.UserRole;
import org.vippro.user_service.repository.UserAccountRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountServiceTest {

    private final UserAccountRepository repository =
            mock(UserAccountRepository.class);
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();
    private final UserAccountService service =
            new UserAccountService(repository, passwordEncoder);

    @Test
    void createsUserWithNormalizedIdentityAndHashedPassword() {
        CreateUserRequest request = new CreateUserRequest(
                " Alice ",
                " Alice@Example.COM ",
                "a-strong-password",
                "Alice Nguyen"
        );
        when(repository.saveAndFlush(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount user = service.create(request);

        assertEquals("alice", user.getUsername());
        assertEquals("alice@example.com", user.getEmail());
        assertTrue(passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        ));
        assertEquals(java.util.Set.of(UserRole.USER), user.getRoles());
    }

    @Test
    void rejectsDuplicateUsername() {
        when(repository.existsByUsernameIgnoreCase("alice"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> service.create(new CreateUserRequest(
                        "alice",
                        "alice@example.com",
                        "a-strong-password",
                        "Alice Nguyen"
                ))
        );
    }

    @Test
    void translatesDatabaseIdentityConflict() {
        when(repository.saveAndFlush(any(UserAccount.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> service.create(new CreateUserRequest(
                        "alice",
                        "alice@example.com",
                        "a-strong-password",
                        "Alice Nguyen"
                ))
        );

        assertEquals(
                "Username or email already exists",
                exception.getMessage()
        );
    }

    @Test
    void changesPasswordWhenCurrentPasswordMatches() {
        UUID userId = UUID.randomUUID();
        UserAccount user = UserAccount.builder()
                .userId(userId)
                .username("alice")
                .email("alice@example.com")
                .passwordHash(passwordEncoder.encode("old-password-123"))
                .fullName("Alice Nguyen")
                .build();
        when(repository.findById(userId)).thenReturn(Optional.of(user));

        service.changePassword(
                userId,
                new ChangePasswordRequest(
                        "old-password-123",
                        "new-password-456"
                )
        );

        assertTrue(passwordEncoder.matches(
                "new-password-456",
                user.getPasswordHash()
        ));
        verify(repository).save(user);
    }
}
