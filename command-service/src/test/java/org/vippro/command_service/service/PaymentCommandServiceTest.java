package org.vippro.command_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.vippro.command.CancelPaymentCommand;
import org.vippro.command.InitiatePaymentCommand;
import org.vippro.command_service.model.EventRecord;
import org.vippro.command_service.model.OutboxRecord;
import org.vippro.command_service.model.PaymentRecord;
import org.vippro.command_service.repository.EventRecordRepository;
import org.vippro.command_service.repository.OutboxRecordRepository;
import org.vippro.command_service.repository.PaymentRecordRepository;
import org.vippro.command_service.repository.ProcessedCommandRepository;
import org.vippro.command_service.util.OutboxStatus;
import org.vippro.command_service.util.PaymentStatus;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentCommandServiceTest {

    private final PaymentRecordRepository paymentRepository =
            mock(PaymentRecordRepository.class);
    private final EventRecordRepository eventRepository =
            mock(EventRecordRepository.class);
    private final OutboxRecordRepository outboxRepository =
            mock(OutboxRecordRepository.class);
    private final ProcessedCommandRepository processedCommandRepository =
            mock(ProcessedCommandRepository.class);
    private final PaymentCommandService service = new PaymentCommandService(
            paymentRepository,
            eventRepository,
            outboxRepository,
            processedCommandRepository,
            new ObjectMapper().findAndRegisterModules()
    );

    @Test
    void initiatingPaymentStoresStateEventAndOutbox() {
        InitiatePaymentCommand command = initiateCommand();
        when(processedCommandRepository.insertIfAbsent(
                any(), anyString(), any()
        )).thenReturn(1);
        when(paymentRepository.findByIdempotencyKey(
                command.getIdempotencyKey()
        )).thenReturn(Optional.empty());

        UUID paymentId = service.initiate(UUID.randomUUID(), command);

        ArgumentCaptor<PaymentRecord> paymentCaptor =
                ArgumentCaptor.forClass(PaymentRecord.class);
        ArgumentCaptor<EventRecord> eventCaptor =
                ArgumentCaptor.forClass(EventRecord.class);
        ArgumentCaptor<OutboxRecord> outboxCaptor =
                ArgumentCaptor.forClass(OutboxRecord.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        verify(eventRepository).save(eventCaptor.capture());
        verify(outboxRepository).save(outboxCaptor.capture());

        assertEquals(paymentId, paymentCaptor.getValue().getPaymentId());
        assertEquals(PaymentStatus.INITIATED,
                paymentCaptor.getValue().getStatus());
        assertEquals("PaymentInitiated",
                eventCaptor.getValue().getEventType());
        assertEquals(1, eventCaptor.getValue().getVersion());
        assertEquals(eventCaptor.getValue().getEventId(),
                outboxCaptor.getValue().getEventId());
        assertEquals("payment-events", outboxCaptor.getValue().getTopic());
        assertEquals(OutboxStatus.NEW,
                outboxCaptor.getValue().getOutboxStatus());
    }

    @Test
    void duplicateCommandDoesNotCreateAnotherEvent() {
        InitiatePaymentCommand command = initiateCommand();
        UUID paymentId = UUID.randomUUID();
        when(processedCommandRepository.insertIfAbsent(
                any(), anyString(), any()
        )).thenReturn(0);
        when(paymentRepository.findByIdempotencyKey(
                command.getIdempotencyKey()
        )).thenReturn(Optional.of(PaymentRecord.builder()
                .paymentId(paymentId)
                .build()));

        assertEquals(
                paymentId,
                service.initiate(UUID.randomUUID(), command)
        );
        verifyNoInteractions(eventRepository, outboxRepository);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void cannotCancelCompletedPayment() {
        UUID paymentId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        PaymentRecord payment = PaymentRecord.builder()
                .paymentId(paymentId)
                .correlationId(correlationId)
                .status(PaymentStatus.COMPLETED)
                .build();
        when(processedCommandRepository.insertIfAbsent(
                any(), anyString(), any()
        )).thenReturn(1);
        when(paymentRepository.findByIdForUpdate(paymentId))
                .thenReturn(Optional.of(payment));

        CancelPaymentCommand command = CancelPaymentCommand.builder()
                .paymentId(paymentId)
                .correlationId(correlationId)
                .reason("late cancellation")
                .cancelledAt(Instant.now())
                .build();

        assertThrows(
                IllegalStateException.class,
                () -> service.cancel(UUID.randomUUID(), command)
        );
        verifyNoInteractions(eventRepository, outboxRepository);
    }

    private InitiatePaymentCommand initiateCommand() {
        return InitiatePaymentCommand.builder()
                .requesterUserId(UUID.randomUUID())
                .sourceAccountId(UUID.randomUUID())
                .destinationAccountId(UUID.randomUUID())
                .correlationId(UUID.randomUUID())
                .amount(new BigDecimal("25.00"))
                .currency(CurrencyType.USD)
                .idempotencyKey(UUID.randomUUID())
                .description("invoice")
                .build();
    }
}
