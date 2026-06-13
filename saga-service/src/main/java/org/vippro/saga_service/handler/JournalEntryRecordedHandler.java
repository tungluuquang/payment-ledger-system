package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.vippro.event.JournalEntryRecorded;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.service.SagaStateService;

@Component
@RequiredArgsConstructor
public class JournalEntryRecordedHandler implements SagaEventHandler<JournalEntryRecorded> {

    private final SagaStateService saga;

    @Override
    public Class<JournalEntryRecorded> eventType() {
        return JournalEntryRecorded.class;
    }

    @Override
    public void handle(JournalEntryRecorded event) {
        SagaState sagaState = saga.find(event.getPaymentId());

    }
}
