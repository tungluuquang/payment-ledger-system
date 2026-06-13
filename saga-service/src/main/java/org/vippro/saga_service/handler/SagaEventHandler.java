package org.vippro.saga_service.handler;

public interface SagaEventHandler<T> {
    Class<T> eventType();
    void handle(T event);
}
