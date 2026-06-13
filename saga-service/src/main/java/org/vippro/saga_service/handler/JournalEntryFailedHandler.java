package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.vippro.event.JournalEntryFailed;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.service.SagaStateService;

@Component
@RequiredArgsConstructor
public class JournalEntryFailedHandler implements SagaEventHandler<JournalEntryFailed> {

    private final SagaStateService saga;

    @Override
    public Class<JournalEntryFailed> eventType() {
        return JournalEntryFailed.class;
    }

    @Override
    public void handle(JournalEntryFailed event) {
        SagaState sagaState = saga.find(event.getPaymentId());

    }
}
