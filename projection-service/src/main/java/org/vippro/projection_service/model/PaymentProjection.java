package org.vippro.projection_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_projections")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentProjection {

    @Id
    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "source_account_id")
    private UUID sourceAccountId;

    @Column(name = "destination_account_id")
    private UUID destinationAccountId;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private CurrencyType currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentViewStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "fraud_status", nullable = false)
    private StepViewStatus fraudStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "debit_status", nullable = false)
    private StepViewStatus debitStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "ledger_status", nullable = false)
    private StepViewStatus ledgerStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "reversal_status", nullable = false)
    private StepViewStatus reversalStatus;

    @Column(name = "debit_transaction_id")
    private UUID debitTransactionId;

    @Column(name = "reversal_transaction_id")
    private UUID reversalTransactionId;

    @Column(name = "journal_entry_id")
    private UUID journalEntryId;

    @Column(name = "reversal_journal_entry_id")
    private UUID reversalJournalEntryId;

    @Column(name = "last_event_type", nullable = false)
    private String lastEventType;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "initiated_at")
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "last_event_at", nullable = false)
    private Instant lastEventAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;
}
