package org.vippro.account_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.account_service.entity.Account;
import org.vippro.account_service.entity.AccountTransaction;
import org.vippro.account_service.enums.AccountStatus;
import org.vippro.account_service.enums.AccountTransactionType;
import org.vippro.account_service.repository.AccountRepository;
import org.vippro.account_service.repository.AccountTransactionRepository;
import org.vippro.command.AccountDebitRequestedCommand;
import org.vippro.command.AccountCreditRequestedCommand;
import org.vippro.command.ReverseAccountDebitCommand;
import org.vippro.command.ReverseAccountCreditCommand;
import org.vippro.event.AccountCredited;
import org.vippro.event.AccountCreditFailed;
import org.vippro.event.AccountCreditReversed;
import org.vippro.event.AccountDebitFailed;
import org.vippro.event.AccountDebitReversed;
import org.vippro.event.AccountDebited;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountCommandService {

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final ProcessedCommandService processedCommandService;
    private final EventOutboxService eventOutboxService;

    @Transactional
    public void debit(
            UUID commandId,
            AccountDebitRequestedCommand command
    ) {
        if (commandId == null) {
            throw new IllegalArgumentException("commandId is required");
        }
        validateDebitCommand(command);

        if (!processedCommandService.tryProcess(
                commandId,
                AccountDebitRequestedCommand.class.getSimpleName()
        )) {
            return;
        }

        AccountTransaction existingDebit = transactionRepository
                .findByIdempotencyKey(command.getIdempotencyKey())
                .orElse(null);

        if (existingDebit != null) {
            validateExistingDebit(existingDebit, command);
            publishDebited(command, existingDebit.getTransactionId());
            return;
        }

        Account account = accountRepository
                .findByIdForUpdate(command.getAccountId())
                .orElse(null);

        if (account == null) {
            publishDebitFailed(command, "ACCOUNT_NOT_FOUND", "Account not found");
            return;
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            publishDebitFailed(command, "ACCOUNT_NOT_ACTIVE", "Account is not active");
            return;
        }

        if (!account.getOwnerUserId().equals(command.getOwnerUserId())) {
            publishDebitFailed(
                    command,
                    "ACCOUNT_OWNERSHIP_MISMATCH",
                    "Source account does not belong to requester"
            );
            return;
        }

        if (account.getCurrency() != command.getCurrency()) {
            publishDebitFailed(command, "CURRENCY_MISMATCH", "Account currency does not match");
            return;
        }

        if (account.getBalance().compareTo(command.getAmount()) < 0) {
            publishDebitFailed(command, "INSUFFICIENT_FUNDS", "Insufficient account balance");
            return;
        }

        UUID transactionId = UUID.randomUUID();
        account.setBalance(account.getBalance().subtract(command.getAmount()));
        accountRepository.save(account);

        transactionRepository.save(
                AccountTransaction.builder()
                        .transactionId(transactionId)
                        .accountId(command.getAccountId())
                        .paymentId(command.getPaymentId())
                        .type(AccountTransactionType.DEBIT)
                        .amount(command.getAmount())
                        .currency(command.getCurrency())
                        .idempotencyKey(command.getIdempotencyKey())
                        .correlationId(command.getCorrelationId())
                        .createdAt(Instant.now())
                        .build()
        );

        publishDebited(command, transactionId);
    }

    @Transactional
    public void credit(UUID commandId, AccountCreditRequestedCommand command) {
        requireValidCredit(commandId, command);
        if (!processedCommandService.tryProcess(
                commandId,
                AccountCreditRequestedCommand.class.getSimpleName()
        )) {
            return;
        }

        AccountTransaction existing = transactionRepository
                .findByIdempotencyKey(command.getIdempotencyKey())
                .orElse(null);
        if (existing != null) {
            validateTransaction(existing, command.getAccountId(),
                    command.getPaymentId(), command.getAmount(),
                    command.getCurrency(), command.getCorrelationId(),
                    AccountTransactionType.CREDIT);
            publishCredited(command, existing.getTransactionId());
            return;
        }

        Account account = accountRepository
                .findByIdForUpdate(command.getAccountId())
                .orElse(null);
        if (account == null) {
            publishCreditFailed(command, "ACCOUNT_NOT_FOUND", "Account not found");
            return;
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            publishCreditFailed(command, "ACCOUNT_NOT_ACTIVE", "Account is not active");
            return;
        }
        if (account.getCurrency() != command.getCurrency()) {
            publishCreditFailed(command, "CURRENCY_MISMATCH",
                    "Account currency does not match");
            return;
        }

        UUID transactionId = UUID.randomUUID();
        account.setBalance(account.getBalance().add(command.getAmount()));
        accountRepository.save(account);
        transactionRepository.save(AccountTransaction.builder()
                .transactionId(transactionId)
                .accountId(command.getAccountId())
                .paymentId(command.getPaymentId())
                .type(AccountTransactionType.CREDIT)
                .amount(command.getAmount())
                .currency(command.getCurrency())
                .idempotencyKey(command.getIdempotencyKey())
                .correlationId(command.getCorrelationId())
                .createdAt(Instant.now())
                .build());
        publishCredited(command, transactionId);
    }

    @Transactional
    public void reverse(
            UUID commandId,
            ReverseAccountDebitCommand command
    ) {
        if (commandId == null) {
            throw new IllegalArgumentException("commandId is required");
        }
        validateReverseCommand(command);

        if (!processedCommandService.tryProcess(
                commandId,
                ReverseAccountDebitCommand.class.getSimpleName()
        )) {
            return;
        }

        AccountTransaction existingReversal = transactionRepository
                .findByOriginalTransactionId(command.getOriginalTransactionId())
                .orElse(null);

        if (existingReversal != null) {
            validateExistingReversal(existingReversal, command);
            publishDebitReversed(command, existingReversal.getTransactionId());
            return;
        }

        AccountTransaction original = transactionRepository
                .findById(command.getOriginalTransactionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Original debit transaction not found"
                ));

        validateOriginalDebit(original, command);

        Account account = accountRepository
                .findByIdForUpdate(command.getAccountId())
                .orElseThrow(() -> new IllegalStateException(
                        "Account not found for debit reversal"
                ));

        if (account.getCurrency() != command.getCurrency()) {
            throw new IllegalStateException(
                    "Account currency does not match reversal command"
            );
        }

        UUID reversalTransactionId = UUID.randomUUID();
        account.setBalance(account.getBalance().add(command.getAmount()));
        accountRepository.save(account);

        transactionRepository.save(
                AccountTransaction.builder()
                        .transactionId(reversalTransactionId)
                        .accountId(command.getAccountId())
                        .paymentId(command.getPaymentId())
                        .type(AccountTransactionType.DEBIT_REVERSAL)
                        .amount(command.getAmount())
                        .currency(command.getCurrency())
                        .originalTransactionId(command.getOriginalTransactionId())
                        .idempotencyKey(command.getIdempotencyKey())
                        .correlationId(command.getCorrelationId())
                        .reason(command.getReason())
                        .createdAt(Instant.now())
                        .build()
        );

        publishDebitReversed(command, reversalTransactionId);
    }

    @Transactional
    public void reverseCredit(
            UUID commandId,
            ReverseAccountCreditCommand command
    ) {
        requireValidCreditReversal(commandId, command);
        if (!processedCommandService.tryProcess(
                commandId,
                ReverseAccountCreditCommand.class.getSimpleName()
        )) {
            return;
        }

        AccountTransaction existing = transactionRepository
                .findByOriginalTransactionId(command.getOriginalTransactionId())
                .orElse(null);
        if (existing != null) {
            validateTransaction(existing, command.getAccountId(),
                    command.getPaymentId(), command.getAmount(),
                    command.getCurrency(), command.getCorrelationId(),
                    AccountTransactionType.CREDIT_REVERSAL);
            publishCreditReversed(command, existing.getTransactionId());
            return;
        }

        AccountTransaction original = transactionRepository
                .findById(command.getOriginalTransactionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Original credit transaction not found"
                ));
        validateTransaction(original, command.getAccountId(),
                command.getPaymentId(), command.getAmount(),
                command.getCurrency(), command.getCorrelationId(),
                AccountTransactionType.CREDIT);

        Account account = accountRepository
                .findByIdForUpdate(command.getAccountId())
                .orElseThrow(() -> new IllegalStateException(
                        "Account not found for credit reversal"
                ));
        if (account.getCurrency() != command.getCurrency()
                || account.getBalance().compareTo(command.getAmount()) < 0) {
            throw new IllegalStateException(
                    "Destination account cannot reverse original credit"
            );
        }

        UUID reversalId = UUID.randomUUID();
        account.setBalance(account.getBalance().subtract(command.getAmount()));
        accountRepository.save(account);
        transactionRepository.save(AccountTransaction.builder()
                .transactionId(reversalId)
                .accountId(command.getAccountId())
                .paymentId(command.getPaymentId())
                .type(AccountTransactionType.CREDIT_REVERSAL)
                .amount(command.getAmount())
                .currency(command.getCurrency())
                .originalTransactionId(command.getOriginalTransactionId())
                .idempotencyKey(command.getIdempotencyKey())
                .correlationId(command.getCorrelationId())
                .reason(command.getReason())
                .createdAt(Instant.now())
                .build());
        publishCreditReversed(command, reversalId);
    }

    private void publishCredited(
            AccountCreditRequestedCommand command,
            UUID transactionId
    ) {
        UUID eventId = UUID.randomUUID();
        eventOutboxService.save(eventId, command.getAccountId(),
                AccountCredited.builder()
                        .eventId(eventId)
                        .paymentId(command.getPaymentId())
                        .accountId(command.getAccountId())
                        .transactionId(transactionId)
                        .amount(command.getAmount())
                        .currency(command.getCurrency())
                        .correlationId(command.getCorrelationId())
                        .occurredAt(Instant.now())
                        .build());
    }

    private void publishCreditFailed(
            AccountCreditRequestedCommand command,
            String errorCode,
            String reason
    ) {
        UUID eventId = UUID.randomUUID();
        eventOutboxService.save(eventId, command.getAccountId(),
                AccountCreditFailed.builder()
                        .eventId(eventId)
                        .paymentId(command.getPaymentId())
                        .accountId(command.getAccountId())
                        .amount(command.getAmount())
                        .currency(command.getCurrency())
                        .correlationId(command.getCorrelationId())
                        .errorCode(errorCode)
                        .reason(reason)
                        .occurredAt(Instant.now())
                        .build());
    }

    private void publishCreditReversed(
            ReverseAccountCreditCommand command,
            UUID reversalTransactionId
    ) {
        UUID eventId = UUID.randomUUID();
        eventOutboxService.save(eventId, command.getAccountId(),
                AccountCreditReversed.builder()
                        .eventId(eventId)
                        .paymentId(command.getPaymentId())
                        .accountId(command.getAccountId())
                        .originalTransactionId(command.getOriginalTransactionId())
                        .reversalTransactionId(reversalTransactionId)
                        .amount(command.getAmount())
                        .currency(command.getCurrency())
                        .correlationId(command.getCorrelationId())
                        .occurredAt(Instant.now())
                        .build());
    }

    private void publishDebitFailed(
            AccountDebitRequestedCommand command,
            String errorCode,
            String reason
    ) {
        UUID eventId = UUID.randomUUID();
        AccountDebitFailed event = AccountDebitFailed.builder()
                .eventId(eventId)
                .paymentId(command.getPaymentId())
                .accountId(command.getAccountId())
                .amount(command.getAmount())
                .currency(command.getCurrency())
                .correlationId(command.getCorrelationId())
                .errorCode(errorCode)
                .reason(reason)
                .occurredAt(Instant.now())
                .build();

        eventOutboxService.save(eventId, command.getAccountId(), event);
    }

    private void publishDebited(
            AccountDebitRequestedCommand command,
            UUID transactionId
    ) {
        UUID eventId = UUID.randomUUID();
        AccountDebited event = AccountDebited.builder()
                .eventId(eventId)
                .paymentId(command.getPaymentId())
                .accountId(command.getAccountId())
                .amount(command.getAmount())
                .currency(command.getCurrency())
                .transactionId(transactionId)
                .correlationId(command.getCorrelationId())
                .occurredAt(Instant.now())
                .build();

        eventOutboxService.save(eventId, command.getAccountId(), event);
    }

    private void publishDebitReversed(
            ReverseAccountDebitCommand command,
            UUID reversalTransactionId
    ) {
        UUID eventId = UUID.randomUUID();
        AccountDebitReversed event = AccountDebitReversed.builder()
                .eventId(eventId)
                .paymentId(command.getPaymentId())
                .accountId(command.getAccountId())
                .originalTransactionId(command.getOriginalTransactionId())
                .reversalTransactionId(reversalTransactionId)
                .amount(command.getAmount())
                .currency(command.getCurrency())
                .correlationId(command.getCorrelationId())
                .occurredAt(Instant.now())
                .build();

        eventOutboxService.save(eventId, command.getAccountId(), event);
    }

    private void validateDebitCommand(AccountDebitRequestedCommand command) {
        if (command == null
                || command.getPaymentId() == null
                || command.getAccountId() == null
                || command.getOwnerUserId() == null
                || command.getAmount() == null
                || command.getAmount().signum() <= 0
                || command.getCurrency() == null
                || command.getIdempotencyKey() == null
                || command.getCorrelationId() == null) {
            throw new IllegalArgumentException(
                    "Invalid AccountDebitRequestedCommand"
            );
        }
    }

    private void requireValidCredit(
            UUID commandId,
            AccountCreditRequestedCommand command
    ) {
        if (commandId == null
                || command == null
                || command.getPaymentId() == null
                || command.getAccountId() == null
                || command.getAmount() == null
                || command.getAmount().signum() <= 0
                || command.getCurrency() == null
                || command.getIdempotencyKey() == null
                || command.getCorrelationId() == null) {
            throw new IllegalArgumentException(
                    "Invalid AccountCreditRequestedCommand"
            );
        }
    }

    private void requireValidCreditReversal(
            UUID commandId,
            ReverseAccountCreditCommand command
    ) {
        if (commandId == null
                || command == null
                || command.getPaymentId() == null
                || command.getAccountId() == null
                || command.getOriginalTransactionId() == null
                || command.getAmount() == null
                || command.getAmount().signum() <= 0
                || command.getCurrency() == null
                || command.getIdempotencyKey() == null
                || command.getCorrelationId() == null) {
            throw new IllegalArgumentException(
                    "Invalid ReverseAccountCreditCommand"
            );
        }
    }

    private void validateTransaction(
            AccountTransaction transaction,
            UUID accountId,
            UUID paymentId,
            java.math.BigDecimal amount,
            org.vippro.util.CurrencyType currency,
            UUID correlationId,
            AccountTransactionType type
    ) {
        if (transaction.getType() != type
                || !Objects.equals(transaction.getAccountId(), accountId)
                || !Objects.equals(transaction.getPaymentId(), paymentId)
                || transaction.getAmount().compareTo(amount) != 0
                || transaction.getCurrency() != currency
                || !Objects.equals(transaction.getCorrelationId(), correlationId)) {
            throw new IllegalStateException(
                    "Account transaction does not match command"
            );
        }
    }

    private void validateReverseCommand(ReverseAccountDebitCommand command) {
        if (command == null
                || command.getPaymentId() == null
                || command.getAccountId() == null
                || command.getOriginalTransactionId() == null
                || command.getAmount() == null
                || command.getAmount().signum() <= 0
                || command.getCurrency() == null
                || command.getIdempotencyKey() == null
                || command.getCorrelationId() == null) {
            throw new IllegalArgumentException(
                    "Invalid ReverseAccountDebitCommand"
            );
        }
    }

    private void validateOriginalDebit(
            AccountTransaction original,
            ReverseAccountDebitCommand command
    ) {
        if (original.getType() != AccountTransactionType.DEBIT
                || !Objects.equals(original.getAccountId(), command.getAccountId())
                || !Objects.equals(original.getPaymentId(), command.getPaymentId())
                || original.getAmount().compareTo(command.getAmount()) != 0
                || original.getCurrency() != command.getCurrency()
                || !Objects.equals(original.getCorrelationId(), command.getCorrelationId())) {
            throw new IllegalStateException(
                    "ReverseAccountDebitCommand does not match original debit"
            );
        }
    }

    private void validateExistingDebit(
            AccountTransaction debit,
            AccountDebitRequestedCommand command
    ) {
        if (debit.getType() != AccountTransactionType.DEBIT
                || !Objects.equals(debit.getAccountId(), command.getAccountId())
                || !Objects.equals(debit.getPaymentId(), command.getPaymentId())
                || debit.getAmount().compareTo(command.getAmount()) != 0
                || debit.getCurrency() != command.getCurrency()
                || !Objects.equals(debit.getCorrelationId(), command.getCorrelationId())) {
            throw new IllegalStateException(
                    "Idempotency key belongs to a different debit"
            );
        }
    }

    private void validateExistingReversal(
            AccountTransaction reversal,
            ReverseAccountDebitCommand command
    ) {
        if (reversal.getType() != AccountTransactionType.DEBIT_REVERSAL
                || !Objects.equals(reversal.getAccountId(), command.getAccountId())
                || !Objects.equals(reversal.getPaymentId(), command.getPaymentId())
                || reversal.getAmount().compareTo(command.getAmount()) != 0
                || reversal.getCurrency() != command.getCurrency()
                || !Objects.equals(reversal.getCorrelationId(), command.getCorrelationId())) {
            throw new IllegalStateException(
                    "Existing debit reversal does not match command"
            );
        }
    }
}
