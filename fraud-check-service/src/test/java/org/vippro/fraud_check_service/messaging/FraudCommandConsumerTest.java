package org.vippro.fraud_check_service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.vippro.command.FraudCheckRequestedCommand;
import org.vippro.fraud_check_service.service.FraudCheckService;
import org.vippro.messaging.CommandEnvelope;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FraudCommandConsumerTest {

    @Test
    void deserializesEnvelopeAndDispatchesFraudCommand() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        FraudCheckService service = mock(FraudCheckService.class);
        FraudCommandConsumer consumer =
                new FraudCommandConsumer(objectMapper, service);

        UUID commandId = UUID.randomUUID();
        FraudCheckRequestedCommand command =
                FraudCheckRequestedCommand.builder()
                        .paymentId(UUID.randomUUID())
                        .accountId(UUID.randomUUID())
                        .amount(new BigDecimal("25.00"))
                        .currency(CurrencyType.USD)
                        .idempotencyKey(UUID.randomUUID())
                        .correlationId(UUID.randomUUID())
                        .build();
        CommandEnvelope envelope = CommandEnvelope.builder()
                .commandId(commandId)
                .sagaId(command.getPaymentId())
                .commandType(
                        FraudCheckRequestedCommand.class.getSimpleName()
                )
                .payload(objectMapper.valueToTree(command))
                .createdAt(Instant.now())
                .build();

        consumer.consume(objectMapper.writeValueAsString(envelope));

        ArgumentCaptor<FraudCheckRequestedCommand> captor =
                ArgumentCaptor.forClass(FraudCheckRequestedCommand.class);
        verify(service).check(
                org.mockito.ArgumentMatchers.eq(commandId),
                captor.capture()
        );
        assertEquals(command.getPaymentId(), captor.getValue().getPaymentId());
        assertEquals(0, command.getAmount().compareTo(
                captor.getValue().getAmount()
        ));
    }
}
