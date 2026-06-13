package org.vippro.saga_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.saga_service.repository.ProcessedEventRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessedEventService {
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public boolean tryProcess(UUID eventId, String eventType) {
        int inserted = processedEventRepository.insertIfAbsent(
                eventId,
                eventType,
                Instant.now()
        );

        if (inserted == 0) {
            log.warn(
                    "Duplicate event received: {}", eventId
            );
        }

        return inserted == 1;
    }
}
