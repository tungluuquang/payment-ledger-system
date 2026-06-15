package org.vippro.audit_service.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.vippro.audit_service.service.AuditIngestionService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditEventConsumerTest {

    @Test
    void forwardsCompleteKafkaRecordToAuditService() {
        AuditIngestionService service = mock(AuditIngestionService.class);
        AuditEventConsumer consumer = new AuditEventConsumer(service);
        ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                "payment-events",
                2,
                42,
                "payment",
                new Object()
        );

        consumer.consume(record);

        verify(service).record(record);
    }
}
