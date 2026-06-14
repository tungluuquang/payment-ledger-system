package org.vippro.command_service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.vippro.command.CompletePaymentCommand;
import org.vippro.command_service.service.PaymentCommandService;
import org.vippro.messaging.CommandEnvelope;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentCommandConsumerTest {

    @Test
    void deserializesEnvelopeAndDispatchesCompleteCommand() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        PaymentCommandService service = mock(PaymentCommandService.class);
        PaymentCommandConsumer consumer =
                new PaymentCommandConsumer(objectMapper, service);

        UUID commandId = UUID.randomUUID();
        CompletePaymentCommand command = CompletePaymentCommand.builder()
                .paymentId(UUID.randomUUID())
                .correlationId(UUID.randomUUID())
                .completedAt(Instant.now())
                .build();
        CommandEnvelope envelope = CommandEnvelope.builder()
                .commandId(commandId)
                .sagaId(command.getPaymentId())
                .commandType(CompletePaymentCommand.class.getSimpleName())
                .payload(objectMapper.valueToTree(command))
                .createdAt(Instant.now())
                .build();

        consumer.consume(objectMapper.writeValueAsString(envelope));

        ArgumentCaptor<CompletePaymentCommand> captor =
                ArgumentCaptor.forClass(CompletePaymentCommand.class);
        verify(service).complete(
                org.mockito.ArgumentMatchers.eq(commandId),
                captor.capture()
        );
        assertEquals(command.getPaymentId(), captor.getValue().getPaymentId());
        assertEquals(command.getCorrelationId(),
                captor.getValue().getCorrelationId());
    }
}
