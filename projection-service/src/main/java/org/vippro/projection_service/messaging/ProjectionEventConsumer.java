package org.vippro.projection_service.messaging;

import lombok.RequiredArgsConstructor;
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
    public void consume(Object event) {
        paymentProjectionService.project(event);
    }
}
