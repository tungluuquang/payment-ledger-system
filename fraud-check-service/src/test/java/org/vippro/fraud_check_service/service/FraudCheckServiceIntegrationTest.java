package org.vippro.fraud_check_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.command.FraudCheckRequestedCommand;
import org.vippro.fraud_check_service.model.FraudDecision;
import org.vippro.fraud_check_service.model.FraudDecisionStatus;
import org.vippro.fraud_check_service.repository.EventOutboxRepository;
import org.vippro.fraud_check_service.repository.FraudDecisionRepository;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FraudCheckServiceIntegrationTest {

    @Autowired
    private FraudCheckService service;

    @Autowired
    private FraudDecisionRepository decisionRepository;

    @Autowired
    private EventOutboxRepository outboxRepository;

    @Test
    void passesPaymentAndSuppressesDuplicateCommand() {
        FraudCheckRequestedCommand command =
                command(UUID.randomUUID(), new BigDecimal("50.00"));
        UUID commandId = UUID.randomUUID();

        service.check(commandId, command);
        service.check(commandId, command);

        FraudDecision decision = decisionRepository
                .findByPaymentId(command.getPaymentId())
                .orElseThrow();
        assertEquals(FraudDecisionStatus.PASSED, decision.getStatus());
        assertEquals("ALL_RULES_PASSED", decision.getRuleCode());
        assertEquals("test-v1", decision.getRuleVersion());
        assertEquals(1, outboxRepository.findAll().size());
        assertEquals(
                "FraudCheckPassed",
                outboxRepository.findAll().getFirst().getEventType()
        );
    }

    @Test
    void failsPaymentAboveCurrencyThreshold() {
        FraudCheckRequestedCommand command =
                command(UUID.randomUUID(), new BigDecimal("100.01"));

        service.check(UUID.randomUUID(), command);

        FraudDecision decision = decisionRepository
                .findByPaymentId(command.getPaymentId())
                .orElseThrow();
        assertEquals(FraudDecisionStatus.FAILED, decision.getStatus());
        assertEquals("AMOUNT_LIMIT_EXCEEDED", decision.getRuleCode());
        assertEquals(
                "FraudCheckFailed",
                outboxRepository.findAll().getFirst().getEventType()
        );
    }

    @Test
    void failsWhenAccountExceedsVelocityLimit() {
        UUID accountId = UUID.randomUUID();
        service.check(
                UUID.randomUUID(),
                command(accountId, new BigDecimal("10.00"))
        );
        service.check(
                UUID.randomUUID(),
                command(accountId, new BigDecimal("20.00"))
        );
        FraudCheckRequestedCommand third =
                command(accountId, new BigDecimal("30.00"));

        service.check(UUID.randomUUID(), third);

        FraudDecision decision = decisionRepository
                .findByPaymentId(third.getPaymentId())
                .orElseThrow();
        assertEquals(FraudDecisionStatus.FAILED, decision.getStatus());
        assertEquals("VELOCITY_LIMIT_EXCEEDED", decision.getRuleCode());
    }

    @Test
    void rejectsReusedIdempotencyKeyWithDifferentPayment() {
        FraudCheckRequestedCommand first =
                command(UUID.randomUUID(), new BigDecimal("10.00"));
        service.check(UUID.randomUUID(), first);
        FraudCheckRequestedCommand conflict =
                FraudCheckRequestedCommand.builder()
                        .paymentId(UUID.randomUUID())
                        .accountId(first.getAccountId())
                        .amount(first.getAmount())
                        .currency(first.getCurrency())
                        .idempotencyKey(first.getIdempotencyKey())
                        .correlationId(first.getCorrelationId())
                        .build();

        assertThrows(
                IllegalStateException.class,
                () -> service.check(UUID.randomUUID(), conflict)
        );
    }

    private FraudCheckRequestedCommand command(
            UUID accountId,
            BigDecimal amount
    ) {
        return FraudCheckRequestedCommand.builder()
                .paymentId(UUID.randomUUID())
                .accountId(accountId)
                .amount(amount)
                .currency(CurrencyType.USD)
                .idempotencyKey(UUID.randomUUID())
                .correlationId(UUID.randomUUID())
                .build();
    }
}
