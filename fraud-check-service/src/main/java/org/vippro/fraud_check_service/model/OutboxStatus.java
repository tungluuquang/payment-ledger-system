package org.vippro.fraud_check_service.model;

public enum OutboxStatus {
    NEW,
    PROCESSING,
    PUBLISHED,
    FAILED
}
