package org.vippro.saga_service.messaging.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.vippro.event.PaymentInitiated;
import org.vippro.saga_service.handler.SagaEventHandler;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SagaEventConsumerTest {

    @Test
    void unwrapsKafkaRecordBeforeDispatching() {
        @SuppressWarnings("unchecked")
        SagaEventHandler<PaymentInitiated> handler =
                mock(SagaEventHandler.class);
        when(handler.eventType()).thenReturn(PaymentInitiated.class);
        PaymentInitiated event = PaymentInitiated.builder().build();
        SagaEventConsumer consumer =
                new SagaEventConsumer(List.of(handler));

        consumer.consume(new ConsumerRecord<>(
                "payment-events",
                0,
                0,
                null,
                event
        ));

        verify(handler).handle(event);
    }
}
