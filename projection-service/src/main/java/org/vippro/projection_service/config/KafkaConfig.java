package org.vippro.projection_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaTemplate<String, Object> projectionDeadLetterKafkaTemplate(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> properties = new HashMap<>(
                kafkaProperties.buildProducerProperties(null)
        );
        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );
        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class
        );
        return new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(properties)
        );
    }

    @Bean
    public DefaultErrorHandler projectionEventErrorHandler(
            KafkaTemplate<String, Object>
                    projectionDeadLetterKafkaTemplate,
            @Value("${projection.kafka.retry.interval-ms:2000}")
            long retryIntervalMs,
            @Value("${projection.kafka.retry.max-attempts:3}")
            long maxAttempts
    ) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        projectionDeadLetterKafkaTemplate,
                        (record, exception) -> new TopicPartition(
                                record.topic() + ".DLT",
                                record.partition()
                        )
                );
        return new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(retryIntervalMs, maxAttempts)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
    projectionEventKafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            DefaultErrorHandler projectionEventErrorHandler,
            @Value("${spring.kafka.listener.auto-startup:true}")
            boolean autoStartup
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(projectionEventErrorHandler);
        factory.setAutoStartup(autoStartup);
        return factory;
    }

    @Bean
    public NewTopic paymentEventsDeadLetterTopic() {
        return deadLetterTopic("payment-events.DLT");
    }

    @Bean
    public NewTopic fraudEventsDeadLetterTopic() {
        return deadLetterTopic("fraud-events.DLT");
    }

    @Bean
    public NewTopic accountEventsDeadLetterTopic() {
        return deadLetterTopic("account-events.DLT");
    }

    @Bean
    public NewTopic ledgerEventsDeadLetterTopic() {
        return deadLetterTopic("ledger-events.DLT");
    }

    private NewTopic deadLetterTopic(String name) {
        return TopicBuilder.name(name)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
