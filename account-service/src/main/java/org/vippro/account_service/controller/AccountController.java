package org.vippro.account_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.vippro.account_service.dto.AccountResponse;
import org.vippro.account_service.dto.CreateAccountRequest;
import org.vippro.account_service.entity.Account;
import org.vippro.account_service.service.AccountManagementService;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountManagementService accountManagementService;

    @GetMapping
    public Page<AccountResponse> list(
            @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size
    ) {
        return accountManagementService.listOwned(
                userId(jwt),
                page,
                size
        ).map(AccountResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(
            @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateAccountRequest request
    ) {
        Account account = accountManagementService.create(
                userId(jwt),
                request.accountId(),
                request.initialBalance(),
                request.currency()
        );
        return AccountResponse.from(account);
    }

    @GetMapping("/{accountId}")
    public AccountResponse find(
            @PathVariable UUID accountId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,
            Authentication authentication
    ) {
        return AccountResponse.from(
                accountManagementService.findOwned(
                        accountId,
                        userId(jwt),
                        authentication.getAuthorities().stream()
                                .anyMatch(authority ->
                                        authority.getAuthority().equals("ROLE_ADMIN"))
                )
        );
    }

    private UUID userId(Jwt jwt) {
        String value = jwt == null ? null : jwt.getClaimAsString("user_id");
        if (value == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "A user access token is required"
            );
        }
        return UUID.fromString(value);
    }
}
