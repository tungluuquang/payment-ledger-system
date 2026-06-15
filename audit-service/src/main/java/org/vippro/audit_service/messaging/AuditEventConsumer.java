package org.vippro.audit_service.messaging;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vippro.audit_service.service.AuditIngestionService;

@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditIngestionService ingestionService;

    @KafkaListener(
            topics = {
                    "payment-events",
                    "fraud-events",
                    "account-events",
                    "ledger-events"
            },
            containerFactory = "auditKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, Object> record) {
        ingestionService.record(record);
    }
}
