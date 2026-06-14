package org.vippro.user_service.dto;

import org.vippro.user_service.model.UserAccount;
import org.vippro.user_service.model.UserRole;
import org.vippro.user_service.model.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        String username,
        String email,
        String fullName,
        UserStatus status,
        Set<UserRole> roles,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus(),
                Set.copyOf(user.getRoles()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
