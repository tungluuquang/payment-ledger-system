package org.vippro.analytics_service.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.vippro.analytics_service.service.PaymentAnalyticsProjector;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AnalyticsEventConsumerTest {

    @Test
    void unwrapsKafkaRecordBeforeProjection() {
        PaymentAnalyticsProjector projector =
                mock(PaymentAnalyticsProjector.class);
        AnalyticsEventConsumer consumer =
                new AnalyticsEventConsumer(projector);
        Object event = new Object();

        consumer.consume(new ConsumerRecord<>(
                "payment-events",
                0,
                0,
                null,
                event
        ));

        verify(projector).project(event);
    }
}
