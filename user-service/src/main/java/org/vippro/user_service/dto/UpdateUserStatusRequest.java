package org.vippro.user_service.dto;

import jakarta.validation.constraints.NotNull;
import org.vippro.user_service.model.UserStatus;

public record UpdateUserStatusRequest(
        @NotNull UserStatus status
) {
}
