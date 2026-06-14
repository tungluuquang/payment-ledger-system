package org.vippro.account_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@RequestBody CreateAccountRequest request) {
        Account account = accountManagementService.create(
                request.accountId(),
                request.initialBalance(),
                request.currency()
        );
        return AccountResponse.from(account);
    }

    @GetMapping("/{accountId}")
    public AccountResponse find(@PathVariable UUID accountId) {
        return AccountResponse.from(
                accountManagementService.find(accountId)
        );
    }
}
