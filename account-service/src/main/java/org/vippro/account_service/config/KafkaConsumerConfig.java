package org.vippro.account_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
public class KafkaConsumerConfig {

    @Bean
    @Primary
    public KafkaTemplate<String, Object> eventKafkaTemplate(
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
    public KafkaTemplate<String, String> deadLetterKafkaTemplate(
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
                StringSerializer.class
        );

        return new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(properties)
        );
    }

    @Bean
    public DefaultErrorHandler accountCommandErrorHandler(
            @Qualifier("deadLetterKafkaTemplate")
            KafkaTemplate<String, String> deadLetterKafkaTemplate,
            @Value("${account.kafka.retry.interval-ms:2000}")
            long retryIntervalMs,
            @Value("${account.kafka.retry.max-attempts:3}")
            long maxAttempts
    ) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        deadLetterKafkaTemplate,
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
    public ConcurrentKafkaListenerContainerFactory<String, String>
    accountCommandKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler accountCommandErrorHandler,
            @Value("${spring.kafka.listener.auto-startup:true}")
            boolean autoStartup
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(accountCommandErrorHandler);
        factory.setAutoStartup(autoStartup);
        return factory;
    }

    @Bean
    public NewTopic accountCommandsDeadLetterTopic() {
        return TopicBuilder.name("account-commands.DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
