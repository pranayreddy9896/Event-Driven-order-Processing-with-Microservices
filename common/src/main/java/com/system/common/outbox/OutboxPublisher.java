package com.system.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.common.model.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Helper to persist events atomically in the outbox table within the caller's active database transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Enqueue an event into the local database outbox table.
     */
    public <T> OutboxEvent enqueue(String topic, String eventType, String aggregateType, String aggregateId, String traceId, T payload) {
        try {
            EventEnvelope<T> envelope = EventEnvelope.<T>builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .timestamp(Instant.now())
                    .traceId(traceId)
                    .payload(payload)
                    .build();

            String jsonPayload = objectMapper.writeValueAsString(envelope);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(envelope.getEventId())
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .topic(topic)
                    .payload(jsonPayload)
                    .traceId(traceId)
                    .status(OutboxStatus.PENDING)
                    .createdAt(Instant.now())
                    .retryCount(0)
                    .build();

            OutboxEvent saved = outboxEventRepository.save(outboxEvent);
            log.info("Saved outbox event [id={}, topic={}, type={}, aggregateId={}]",
                    saved.getId(), topic, eventType, aggregateId);
            return saved;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload: {}", e.getMessage(), e);
            throw new IllegalStateException("Could not serialize event payload", e);
        }
    }
}
