package org.vippro.saga_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "saga_instances",
        indexes = {
                @Index(name = "idx_saga_payment", columnList = "payment_id"),
                @Index(name = "idx_saga_state", columnList = "payment_state")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SagaState {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID sagaId;

    @Column(name = "payment_id", nullable = false, unique = true)
    private UUID paymentId;
    private UUID requesterUserId;
    private UUID sourceAccountId;
    private UUID destinationAccountId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private CurrencyType currency;

    private UUID debitTransactionId;
    private UUID creditTransactionId;
    private UUID reversalTransactionId;
    private UUID creditReversalTransactionId;
    private UUID correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_state", nullable = false)
    private PaymentState paymentState;

    @Enumerated(EnumType.STRING)
    private StepStatus fraudStatus;

    @Enumerated(EnumType.STRING)
    private StepStatus debitStatus;

    @Enumerated(EnumType.STRING)
    private StepStatus creditStatus;

    @Enumerated(EnumType.STRING)
    private StepStatus ledgerStatus;

    @Enumerated(EnumType.STRING)
    private StepStatus debitReversalStatus;

    @Enumerated(EnumType.STRING)
    private StepStatus creditReversalStatus;

    private String lastEventType;
    private String lastError;

    private Integer retryCount;

    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (this.sagaId == null) {
            this.sagaId = UUID.randomUUID();
        }

        this.createdAt = now;
        this.updatedAt = now;

        this.retryCount = 0;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
