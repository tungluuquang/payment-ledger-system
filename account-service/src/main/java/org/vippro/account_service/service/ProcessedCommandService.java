package org.vippro.account_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.account_service.repository.ProcessedCommandRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessedCommandService {

    private final ProcessedCommandRepository processedCommandRepository;

    @Transactional
    public boolean tryProcess(UUID commandId, String commandType) {
        return processedCommandRepository.insertIfAbsent(
                commandId,
                commandType,
                Instant.now()
        ) == 1;
    }
}
