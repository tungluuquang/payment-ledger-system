package org.vippro.fraud_check_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_decisions")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FraudDecision {

    @Id
    @Column(name = "decision_id", nullable = false, updatable = false)
    private UUID decisionId;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private UUID idempotencyKey;

    @Column(nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3, updatable = false)
    private CurrencyType currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private FraudDecisionStatus status;

    @Column(name = "rule_code", nullable = false, updatable = false)
    private String ruleCode;

    @Column(nullable = false, length = 1000, updatable = false)
    private String reason;

    @Column(name = "rule_version", nullable = false, updatable = false)
    private String ruleVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
