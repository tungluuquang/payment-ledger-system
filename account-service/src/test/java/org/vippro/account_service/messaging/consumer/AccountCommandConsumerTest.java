package org.vippro.account_service.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.vippro.account_service.service.AccountCommandService;
import org.vippro.command.AccountDebitRequestedCommand;
import org.vippro.messaging.CommandEnvelope;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AccountCommandConsumerTest {

    @Test
    void deserializesEnvelopeAndDispatchesDebitCommand() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AccountCommandService accountCommandService =
                mock(AccountCommandService.class);
        AccountCommandConsumer consumer = new AccountCommandConsumer(
                objectMapper,
                accountCommandService
        );

        UUID commandId = UUID.randomUUID();
        AccountDebitRequestedCommand command =
                AccountDebitRequestedCommand.builder()
                        .paymentId(UUID.randomUUID())
                        .accountId(UUID.randomUUID())
                        .amount(new BigDecimal("25.00"))
                        .currency(CurrencyType.USD)
                        .idempotencyKey(UUID.randomUUID())
                        .correlationId(UUID.randomUUID())
                        .build();
        CommandEnvelope envelope = CommandEnvelope.builder()
                .commandId(commandId)
                .sagaId(UUID.randomUUID())
                .commandType(
                        AccountDebitRequestedCommand.class.getSimpleName()
                )
                .payload(objectMapper.valueToTree(command))
                .build();

        consumer.consume(objectMapper.writeValueAsString(envelope));

        ArgumentCaptor<AccountDebitRequestedCommand> commandCaptor =
                ArgumentCaptor.forClass(AccountDebitRequestedCommand.class);
        verify(accountCommandService).debit(
                org.mockito.ArgumentMatchers.eq(commandId),
                commandCaptor.capture()
        );

        AccountDebitRequestedCommand actual = commandCaptor.getValue();
        assertEquals(command.getPaymentId(), actual.getPaymentId());
        assertEquals(command.getAccountId(), actual.getAccountId());
        assertEquals(0, command.getAmount().compareTo(actual.getAmount()));
        assertEquals(command.getCurrency(), actual.getCurrency());
        assertEquals(command.getIdempotencyKey(), actual.getIdempotencyKey());
        assertEquals(command.getCorrelationId(), actual.getCorrelationId());
    }
}
