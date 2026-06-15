package org.vippro.saga_service.messaging.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vippro.saga_service.handler.SagaEventHandler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SagaEventConsumer {
    private final Map<Class<?>, SagaEventHandler<?>> handlers;

    public SagaEventConsumer(List<SagaEventHandler<?>> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        SagaEventHandler::eventType,
                        Function.identity()
                ));
    }

    @KafkaListener(
            topics = {
                    "payment-events",
                    "fraud-events",
                    "account-events",
                    "ledger-events"
            },
            groupId = "payment-saga"
    )
    public void consume(ConsumerRecord<String, Object> record) {
        Object event = record.value();
        SagaEventHandler<?> handler = handlers.get(event.getClass());

        if (handler == null) {
            log.warn(
                    "No saga handler registered for event type: {}",
                    event.getClass().getName()
            );
            return;
        }

        dispatch(handler, event);
    }

    @SuppressWarnings("unchecked")
    private <T> void dispatch(
            SagaEventHandler<?> handler,
            Object event
    ) {
        ((SagaEventHandler<T>) handler).handle((T) event);
    }

}
