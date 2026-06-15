package org.vippro.analytics_service.messaging;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vippro.analytics_service.service.PaymentAnalyticsProjector;

@Component
@RequiredArgsConstructor
public class AnalyticsEventConsumer {

    private final PaymentAnalyticsProjector projector;

    @KafkaListener(
            topics = {
                    "payment-events",
                    "fraud-events",
                    "account-events",
                    "ledger-events"
            },
            containerFactory = "analyticsKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, Object> record) {
        projector.project(record.value());
    }
}
