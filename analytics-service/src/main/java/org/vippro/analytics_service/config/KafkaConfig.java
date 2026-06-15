package org.vippro.analytics_service.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    KafkaTemplate<String, Object> analyticsDeadLetterKafkaTemplate(
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
    DefaultErrorHandler analyticsErrorHandler(
            KafkaTemplate<String, Object> analyticsDeadLetterKafkaTemplate,
            @Value("${analytics.kafka.retry.interval-ms:2000}")
            long retryIntervalMs,
            @Value("${analytics.kafka.retry.max-attempts:3}")
            long maxAttempts
    ) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        analyticsDeadLetterKafkaTemplate,
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
    ConcurrentKafkaListenerContainerFactory<String, Object>
    analyticsKafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            DefaultErrorHandler analyticsErrorHandler,
            @Value("${spring.kafka.listener.auto-startup:true}")
            boolean autoStartup
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(analyticsErrorHandler);
        factory.setAutoStartup(autoStartup);
        return factory;
    }
}
