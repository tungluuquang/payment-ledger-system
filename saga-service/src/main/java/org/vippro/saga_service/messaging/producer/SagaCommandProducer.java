package org.vippro.saga_service.messaging.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaCommandProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private void send(String topic, Object command) {
        try {
            String json = objectMapper.writeValueAsString(command);
            log.info("SagaProducer -> Topic: {}, Payload: {}", topic, json);
            kafkaTemplate.send(topic, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize command for topic {}: {}", topic, e.getMessage());
            throw new RuntimeException("Cannot serialize command", e);
        }
    }
}
