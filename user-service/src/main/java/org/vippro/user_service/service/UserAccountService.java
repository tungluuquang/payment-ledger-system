package org.vippro.user_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.user_service.dto.ChangePasswordRequest;
import org.vippro.user_service.dto.CreateUserRequest;
import org.vippro.user_service.dto.UpdateUserRequest;
import org.vippro.user_service.exception.ConflictException;
import org.vippro.user_service.exception.NotFoundException;
import org.vippro.user_service.model.UserAccount;
import org.vippro.user_service.model.UserRole;
import org.vippro.user_service.model.UserStatus;
import org.vippro.user_service.repository.UserAccountRepository;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserAccount create(CreateUserRequest request) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already exists");
        }

        return userRepository.save(
                UserAccount.builder()
                        .userId(UUID.randomUUID())
                        .username(username)
                        .email(email)
                        .passwordHash(
                                passwordEncoder.encode(request.password())
                        )
                        .fullName(request.fullName().trim())
                        .status(UserStatus.ACTIVE)
                        .roles(Set.of(UserRole.USER))
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public UserAccount find(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "User not found: " + userId
                ));
    }

    @Transactional(readOnly = true)
    public Page<UserAccount> list(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "page must be non-negative and size must be between 1 and "
                            + MAX_PAGE_SIZE
            );
        }

        return userRepository.findAll(
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );
    }

    @Transactional
    public UserAccount update(UUID userId, UpdateUserRequest request) {
        UserAccount user = find(userId);
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCaseAndUserIdNot(
                email,
                userId
        )) {
            throw new ConflictException("Email already exists");
        }

        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(
            UUID userId,
            ChangePasswordRequest request
    ) {
        UserAccount user = find(userId);

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "Current password is incorrect"
            );
        }
        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new IllegalArgumentException(
                    "New password must be different"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserAccount updateStatus(UUID userId, UserStatus status) {
        UserAccount user = find(userId);
        user.setStatus(status);
        return userRepository.save(user);
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
