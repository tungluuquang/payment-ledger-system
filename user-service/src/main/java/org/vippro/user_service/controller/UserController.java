package org.vippro.user_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.vippro.user_service.dto.ChangePasswordRequest;
import org.vippro.user_service.dto.CreateUserRequest;
import org.vippro.user_service.dto.UpdateUserRequest;
import org.vippro.user_service.dto.UpdateUserStatusRequest;
import org.vippro.user_service.dto.UserResponse;
import org.vippro.user_service.service.UserAccountService;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserAccountService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return UserResponse.from(userService.create(request));
    }

    @GetMapping("/{userId}")
    @PreAuthorize(
            "hasRole('ADMIN') or #userId.toString() == "
                    + "authentication.tokenAttributes['user_id']"
    )
    public UserResponse find(@PathVariable UUID userId) {
        return UserResponse.from(userService.find(userId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return userService.list(page, size).map(UserResponse::from);
    }

    @PatchMapping("/{userId}")
    @PreAuthorize(
            "hasRole('ADMIN') or #userId.toString() == "
                    + "authentication.tokenAttributes['user_id']"
    )
    public UserResponse update(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return UserResponse.from(userService.update(userId, request));
    }

    @PostMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(
            "hasRole('ADMIN') or #userId.toString() == "
                    + "authentication.tokenAttributes['user_id']"
    )
    public void changePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(userId, request);
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return UserResponse.from(
                userService.updateStatus(userId, request.status())
        );
    }
}
