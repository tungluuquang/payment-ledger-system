package org.vippro.saga_service.model;

public enum PaymentState {
    STARTED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    COMPENSATING,
    COMPENSATED
}
