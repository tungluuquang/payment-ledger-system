package org.vippro.projection_service.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.vippro.projection_service.service.PaymentProjectionService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProjectionEventConsumerTest {

    @Test
    void unwrapsKafkaRecordBeforeProjecting() {
        PaymentProjectionService projectionService =
                mock(PaymentProjectionService.class);
        ProjectionEventConsumer consumer =
                new ProjectionEventConsumer(projectionService);
        Object event = new Object();

        consumer.consume(new ConsumerRecord<>(
                "payment-events",
                0,
                0,
                null,
                event
        ));

        verify(projectionService).project(event);
    }
}
