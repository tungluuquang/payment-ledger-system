package org.vippro.command_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.vippro.command.InitiatePaymentCommand;
import org.vippro.command_service.service.PaymentCommandService;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentCommandController {

    private final PaymentCommandService paymentCommandService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public InitiatePaymentResponse initiate(
            @RequestHeader(
                    name = "X-Command-Id",
                    required = false
            ) UUID commandId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody InitiatePaymentCommand command
    ) {
        UUID requesterUserId = userId(jwt);
        InitiatePaymentCommand authenticatedCommand =
                InitiatePaymentCommand.builder()
                        .requesterUserId(requesterUserId)
                        .sourceAccountId(command.getSourceAccountId())
                        .destinationAccountId(command.getDestinationAccountId())
                        .correlationId(command.getCorrelationId())
                        .amount(command.getAmount())
                        .currency(command.getCurrency())
                        .idempotencyKey(command.getIdempotencyKey())
                        .description(command.getDescription())
                        .build();
        UUID effectiveCommandId = commandId == null
                ? UUID.randomUUID()
                : commandId;
        UUID paymentId = paymentCommandService.initiate(
                effectiveCommandId,
                authenticatedCommand
        );
        return new InitiatePaymentResponse(paymentId, effectiveCommandId);
    }

    public record InitiatePaymentResponse(
            UUID paymentId,
            UUID commandId
    ) {
    }

    private UUID userId(Jwt jwt) {
        String value = jwt == null ? null : jwt.getClaimAsString("user_id");
        if (value == null) {
            throw new AccessDeniedException(
                    "A user access token is required"
            );
        }
        return UUID.fromString(value);
    }
}
