package org.vippro.fraud_check_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.command.FraudCheckRequestedCommand;
import org.vippro.event.FraudCheckFailed;
import org.vippro.event.FraudCheckPassed;
import org.vippro.fraud_check_service.config.FraudRuleProperties;
import org.vippro.fraud_check_service.model.FraudDecision;
import org.vippro.fraud_check_service.model.FraudDecisionStatus;
import org.vippro.fraud_check_service.repository.AccountLockStore;
import org.vippro.fraud_check_service.repository.FraudDecisionRepository;
import org.vippro.fraud_check_service.repository.ProcessedCommandStore;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FraudCheckService {

    private final FraudDecisionRepository decisionRepository;
    private final ProcessedCommandStore processedCommandStore;
    private final AccountLockStore accountLockStore;
    private final FraudRuleEngine ruleEngine;
    private final FraudRuleProperties ruleProperties;
    private final EventOutboxService eventOutboxService;

    @Transactional
    public void check(
            UUID commandId,
            FraudCheckRequestedCommand command
    ) {
        validate(commandId, command);

        if (!processedCommandStore.insertIfAbsent(
                commandId,
                FraudCheckRequestedCommand.class.getSimpleName(),
                Instant.now()
        )) {
            return;
        }

        accountLockStore.lock(command.getAccountId());

        FraudDecision existingByIdempotency = decisionRepository
                .findByIdempotencyKey(command.getIdempotencyKey())
                .orElse(null);
        if (existingByIdempotency != null) {
            validateExisting(existingByIdempotency, command);
            return;
        }

        FraudDecision existingByPayment = decisionRepository
                .findByPaymentId(command.getPaymentId())
                .orElse(null);
        if (existingByPayment != null) {
            validateExisting(existingByPayment, command);
            return;
        }

        FraudRuleEngine.RuleResult result = ruleEngine.evaluate(command);
        Instant occurredAt = Instant.now();
        FraudDecision decision = FraudDecision.builder()
                .decisionId(UUID.randomUUID())
                .paymentId(command.getPaymentId())
                .accountId(command.getAccountId())
                .correlationId(command.getCorrelationId())
                .idempotencyKey(command.getIdempotencyKey())
                .amount(command.getAmount())
                .currency(command.getCurrency())
                .status(result.passed()
                        ? FraudDecisionStatus.PASSED
                        : FraudDecisionStatus.FAILED)
                .ruleCode(result.ruleCode())
                .reason(result.reason())
                .ruleVersion(ruleProperties.getVersion())
                .createdAt(occurredAt)
                .build();
        decisionRepository.save(decision);

        UUID eventId = UUID.randomUUID();
        if (result.passed()) {
            eventOutboxService.save(
                    eventId,
                    command.getPaymentId(),
                    FraudCheckPassed.builder()
                            .eventId(eventId)
                            .paymentId(command.getPaymentId())
                            .accountId(command.getAccountId())
                            .correlationId(command.getCorrelationId())
                            .occurredAt(occurredAt)
                            .build()
            );
        } else {
            eventOutboxService.save(
                    eventId,
                    command.getPaymentId(),
                    FraudCheckFailed.builder()
                            .eventId(eventId)
                            .paymentId(command.getPaymentId())
                            .accountId(command.getAccountId())
                            .reason(result.reason())
                            .correlationId(command.getCorrelationId())
                            .occurredAt(occurredAt)
                            .build()
            );
        }
    }

    private void validate(
            UUID commandId,
            FraudCheckRequestedCommand command
    ) {
        if (commandId == null) {
            throw new IllegalArgumentException("commandId is required");
        }
        if (command == null
                || command.getPaymentId() == null
                || command.getAccountId() == null
                || command.getAmount() == null
                || command.getAmount().signum() <= 0
                || command.getCurrency() == null
                || command.getIdempotencyKey() == null
                || command.getCorrelationId() == null) {
            throw new IllegalArgumentException(
                    "Invalid FraudCheckRequestedCommand"
            );
        }
    }

    private void validateExisting(
            FraudDecision existing,
            FraudCheckRequestedCommand command
    ) {
        if (!Objects.equals(existing.getPaymentId(), command.getPaymentId())
                || !Objects.equals(existing.getAccountId(), command.getAccountId())
                || existing.getAmount().compareTo(command.getAmount()) != 0
                || existing.getCurrency() != command.getCurrency()
                || !Objects.equals(existing.getCorrelationId(), command.getCorrelationId())
                || !Objects.equals(existing.getIdempotencyKey(), command.getIdempotencyKey())) {
            throw new IllegalStateException(
                    "Payment or idempotency key already has another fraud decision"
            );
        }
    }
}
