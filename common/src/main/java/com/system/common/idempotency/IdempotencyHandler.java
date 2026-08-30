package com.system.common.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyHandler {

    private final ProcessedEventRepository processedEventRepository;

    /**
     * Checks if the event was already successfully processed.
     */
    public boolean isAlreadyProcessed(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        return processedEventRepository.existsById(eventId);
    }

    /**
     * Marks an event as processed within the database transaction.
     */
    public void markAsProcessed(String eventId, String eventType, String consumerGroup) {
        try {
            ProcessedEvent record = ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType != null ? eventType : "UNKNOWN")
                    .consumerGroup(consumerGroup != null ? consumerGroup : "default")
                    .processedAt(Instant.now())
                    .build();
            processedEventRepository.save(record);
            log.debug("Marked event [id={}, type={}, consumer={}] as processed", eventId, eventType, consumerGroup);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate insert detected for eventId={}; ignoring duplicate", eventId);
        }
    }

    /**
     * Executes the task only if the event has not been processed yet.
     * Returns true if processed, false if skipped as duplicate.
     */
    @Transactional
    public boolean executeIdempotently(String eventId, String eventType, String consumerGroup, Runnable task) {
        if (isAlreadyProcessed(eventId)) {
            log.warn("DUPLICATE EVENT DETECTED: [id={}, type={}, consumer={}]. Skipping execution to prevent duplicate side-effects.",
                    eventId, eventType, consumerGroup);
            return false;
        }

        task.run();
        markAsProcessed(eventId, eventType, consumerGroup);
        return true;
    }
}
