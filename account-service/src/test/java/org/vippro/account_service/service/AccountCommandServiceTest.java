package org.vippro.account_service.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.vippro.account_service.entity.Account;
import org.vippro.account_service.entity.AccountTransaction;
import org.vippro.account_service.enums.AccountStatus;
import org.vippro.account_service.enums.AccountTransactionType;
import org.vippro.account_service.repository.AccountRepository;
import org.vippro.account_service.repository.AccountTransactionRepository;
import org.vippro.command.AccountDebitRequestedCommand;
import org.vippro.command.ReverseAccountDebitCommand;
import org.vippro.event.AccountDebitFailed;
import org.vippro.event.AccountDebitReversed;
import org.vippro.event.AccountDebited;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountCommandServiceTest {

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final AccountTransactionRepository transactionRepository =
            mock(AccountTransactionRepository.class);
    private final ProcessedCommandService processedCommandService =
            mock(ProcessedCommandService.class);
    private final EventOutboxService eventOutboxService =
            mock(EventOutboxService.class);
    private final AccountCommandService service = new AccountCommandService(
            accountRepository,
            transactionRepository,
            processedCommandService,
            eventOutboxService
    );

    @Test
    void debitsBalanceAndPublishesAccountDebited() {
        UUID commandId = UUID.randomUUID();
        AccountDebitRequestedCommand command = debitCommand();
        Account account = account(command.getAccountId(), "150.00");

        when(processedCommandService.tryProcess(
                commandId,
                AccountDebitRequestedCommand.class.getSimpleName()
        )).thenReturn(true);
        when(transactionRepository.findByIdempotencyKey(
                command.getIdempotencyKey()
        )).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(
                command.getAccountId()
        )).thenReturn(Optional.of(account));

        service.debit(commandId, command);

        assertEquals(new BigDecimal("50.00"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(AccountTransaction.class));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventOutboxService).save(
                any(UUID.class),
                eq(command.getAccountId()),
                eventCaptor.capture()
        );
        AccountDebited event = assertInstanceOf(
                AccountDebited.class,
                eventCaptor.getValue()
        );
        assertEquals(command.getPaymentId(), event.getPaymentId());
        assertEquals(command.getAmount(), event.getAmount());
    }

    @Test
    void insufficientFundsPublishesFailureWithoutChangingBalance() {
        UUID commandId = UUID.randomUUID();
        AccountDebitRequestedCommand command = debitCommand();
        Account account = account(command.getAccountId(), "50.00");

        when(processedCommandService.tryProcess(any(), any())).thenReturn(true);
        when(transactionRepository.findByIdempotencyKey(
                command.getIdempotencyKey()
        )).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(
                command.getAccountId()
        )).thenReturn(Optional.of(account));

        service.debit(commandId, command);

        assertEquals(new BigDecimal("50.00"), account.getBalance());
        verify(accountRepository, never()).save(account);
        verify(transactionRepository, never()).save(any());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventOutboxService).save(
                any(UUID.class),
                eq(command.getAccountId()),
                eventCaptor.capture()
        );
        AccountDebitFailed event = assertInstanceOf(
                AccountDebitFailed.class,
                eventCaptor.getValue()
        );
        assertEquals("INSUFFICIENT_FUNDS", event.getErrorCode());
    }

    @Test
    void duplicateIdempotencyKeyDoesNotDebitAgain() {
        UUID commandId = UUID.randomUUID();
        AccountDebitRequestedCommand command = debitCommand();
        AccountTransaction existing = AccountTransaction.builder()
                .transactionId(UUID.randomUUID())
                .accountId(command.getAccountId())
                .paymentId(command.getPaymentId())
                .type(AccountTransactionType.DEBIT)
                .amount(command.getAmount())
                .currency(command.getCurrency())
                .idempotencyKey(command.getIdempotencyKey())
                .correlationId(command.getCorrelationId())
                .build();

        when(processedCommandService.tryProcess(any(), any())).thenReturn(true);
        when(transactionRepository.findByIdempotencyKey(
                command.getIdempotencyKey()
        )).thenReturn(Optional.of(existing));

        service.debit(commandId, command);

        verify(accountRepository, never()).findByIdForUpdate(any());
        verify(transactionRepository, never()).save(any());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventOutboxService).save(
                any(UUID.class),
                eq(command.getAccountId()),
                eventCaptor.capture()
        );
        AccountDebited event = assertInstanceOf(
                AccountDebited.class,
                eventCaptor.getValue()
        );
        assertEquals(existing.getTransactionId(), event.getTransactionId());
    }

    @Test
    void reversesOriginalDebitAndPublishesConfirmation() {
        UUID commandId = UUID.randomUUID();
        ReverseAccountDebitCommand command = reverseCommand();
        Account account = account(command.getAccountId(), "50.00");
        AccountTransaction original = AccountTransaction.builder()
                .transactionId(command.getOriginalTransactionId())
                .accountId(command.getAccountId())
                .paymentId(command.getPaymentId())
                .type(AccountTransactionType.DEBIT)
                .amount(command.getAmount())
                .currency(command.getCurrency())
                .idempotencyKey(UUID.randomUUID())
                .correlationId(command.getCorrelationId())
                .build();

        when(processedCommandService.tryProcess(any(), any())).thenReturn(true);
        when(transactionRepository.findByOriginalTransactionId(
                command.getOriginalTransactionId()
        )).thenReturn(Optional.empty());
        when(transactionRepository.findById(
                command.getOriginalTransactionId()
        )).thenReturn(Optional.of(original));
        when(accountRepository.findByIdForUpdate(
                command.getAccountId()
        )).thenReturn(Optional.of(account));

        service.reverse(commandId, command);

        assertEquals(new BigDecimal("150.00"), account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(AccountTransaction.class));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventOutboxService).save(
                any(UUID.class),
                eq(command.getAccountId()),
                eventCaptor.capture()
        );
        AccountDebitReversed event = assertInstanceOf(
                AccountDebitReversed.class,
                eventCaptor.getValue()
        );
        assertEquals(
                command.getOriginalTransactionId(),
                event.getOriginalTransactionId()
        );
    }

    private AccountDebitRequestedCommand debitCommand() {
        return AccountDebitRequestedCommand.builder()
                .paymentId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency(CurrencyType.USD)
                .idempotencyKey(UUID.randomUUID())
                .correlationId(UUID.randomUUID())
                .build();
    }

    private ReverseAccountDebitCommand reverseCommand() {
        return ReverseAccountDebitCommand.builder()
                .paymentId(UUID.randomUUID())
                .accountId(UUID.randomUUID())
                .originalTransactionId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency(CurrencyType.USD)
                .idempotencyKey(UUID.randomUUID())
                .correlationId(UUID.randomUUID())
                .reason("ledger failed")
                .build();
    }

    private Account account(UUID accountId, String balance) {
        return Account.builder()
                .accountId(accountId)
                .balance(new BigDecimal(balance))
                .currency(CurrencyType.USD)
                .status(AccountStatus.ACTIVE)
                .build();
    }
}
