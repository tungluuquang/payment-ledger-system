package org.vippro.saga_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name("payment-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fraudEventsTopic() {
        return TopicBuilder.name("fraud-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic accountEventsTopic() {
        return TopicBuilder.name("account-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ledgerEventsTopic() {
        return TopicBuilder.name("ledger-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    // ---------- Command topics (gửi) ----------
    @Bean
    public NewTopic paymentCommandsTopic() {
        return TopicBuilder.name("payment-commands")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fraudCheckCommandsTopic() {
        return TopicBuilder.name("fraud-check-commands")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic accountCommandsTopic() {
        return TopicBuilder.name("account-commands")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ledgerCommandsTopic() {
        return TopicBuilder.name("ledger-commands")
                .partitions(3)
                .replicas(1)
                .build();
    }
}