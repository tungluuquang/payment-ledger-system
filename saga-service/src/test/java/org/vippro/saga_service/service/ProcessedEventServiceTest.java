package org.vippro.saga_service.service;

import org.junit.jupiter.api.Test;
import org.vippro.saga_service.repository.ProcessedEventRepository;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessedEventServiceTest {

    private final ProcessedEventRepository repository =
            mock(ProcessedEventRepository.class);
    private final org.vippro.saga_service.service.ProcessedEventService service =
            new ProcessedEventService(repository);

    @Test
    void returnsTrueWhenEventIsInserted() {
        UUID eventId = UUID.randomUUID();
        when(repository.insertIfAbsent(
                eq(eventId),
                eq("PaymentInitiated"),
                any(Instant.class)
        )).thenReturn(1);

        assertTrue(service.tryProcess(eventId, "PaymentInitiated"));

        verify(repository).insertIfAbsent(
                eq(eventId),
                eq("PaymentInitiated"),
                any(Instant.class)
        );
    }

    @Test
    void returnsFalseWhenEventAlreadyExists() {
        UUID eventId = UUID.randomUUID();
        when(repository.insertIfAbsent(
                eq(eventId),
                eq("PaymentInitiated"),
                any(Instant.class)
        )).thenReturn(0);

        assertFalse(service.tryProcess(eventId, "PaymentInitiated"));
    }
}
