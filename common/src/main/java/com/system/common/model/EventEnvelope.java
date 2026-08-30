package com.system.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard CloudEvents-inspired envelope wrapper for all domain events.
 * Guarantees consistent event metadata across all microservices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope<T> {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String eventType;

    private String aggregateType;

    private String aggregateId;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private String traceId;

    private String spanId;

    private T payload;

    public static <T> EventEnvelope<T> of(String eventType, String aggregateType, String aggregateId, String traceId, T payload) {
        return EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .timestamp(Instant.now())
                .traceId(traceId)
                .payload(payload)
                .build();
    }
}
