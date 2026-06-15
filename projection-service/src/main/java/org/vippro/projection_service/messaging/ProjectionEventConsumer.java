package org.vippro.projection_service.messaging;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vippro.projection_service.service.PaymentProjectionService;

@Component
@RequiredArgsConstructor
public class ProjectionEventConsumer {

    private final PaymentProjectionService paymentProjectionService;

    @KafkaListener(
            topics = {
                    "payment-events",
                    "fraud-events",
                    "account-events",
                    "ledger-events"
            },
            containerFactory =
                    "projectionEventKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, Object> record) {
        paymentProjectionService.project(record.value());
    }
}
