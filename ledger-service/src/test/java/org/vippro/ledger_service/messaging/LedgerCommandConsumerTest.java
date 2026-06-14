package org.vippro.ledger_service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.vippro.command.JournalEntryRequestedCommand;
import org.vippro.ledger_service.service.LedgerCommandService;
import org.vippro.messaging.CommandEnvelope;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LedgerCommandConsumerTest {

    @Test
    void deserializesEnvelopeAndDispatchesJournalCommand() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        LedgerCommandService service = mock(LedgerCommandService.class);
        LedgerCommandConsumer consumer =
                new LedgerCommandConsumer(objectMapper, service);

        UUID commandId = UUID.randomUUID();
        JournalEntryRequestedCommand command =
                JournalEntryRequestedCommand.builder()
                        .paymentId(UUID.randomUUID())
                        .debitAccountId(UUID.randomUUID())
                        .creditAccountId(UUID.randomUUID())
                        .correlationId(UUID.randomUUID())
                        .idempotencyKey(UUID.randomUUID())
                        .amount(new BigDecimal("10.00"))
                        .currency(CurrencyType.USD)
                        .build();
        CommandEnvelope envelope = CommandEnvelope.builder()
                .commandId(commandId)
                .sagaId(command.getPaymentId())
                .commandType(
                        JournalEntryRequestedCommand.class.getSimpleName()
                )
                .payload(objectMapper.valueToTree(command))
                .createdAt(Instant.now())
                .build();

        consumer.consume(objectMapper.writeValueAsString(envelope));

        ArgumentCaptor<JournalEntryRequestedCommand> captor =
                ArgumentCaptor.forClass(
                        JournalEntryRequestedCommand.class
                );
        verify(service).record(
                org.mockito.ArgumentMatchers.eq(commandId),
                captor.capture()
        );
        assertEquals(command.getPaymentId(), captor.getValue().getPaymentId());
        assertEquals(0, command.getAmount().compareTo(
                captor.getValue().getAmount()
        ));
    }
}
