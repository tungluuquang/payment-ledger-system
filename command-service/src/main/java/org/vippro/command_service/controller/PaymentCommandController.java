package org.vippro.command_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
            @RequestBody InitiatePaymentCommand command
    ) {
        UUID effectiveCommandId = commandId == null
                ? UUID.randomUUID()
                : commandId;
        UUID paymentId = paymentCommandService.initiate(
                effectiveCommandId,
                command
        );
        return new InitiatePaymentResponse(paymentId, effectiveCommandId);
    }

    public record InitiatePaymentResponse(
            UUID paymentId,
            UUID commandId
    ) {
    }
}
