package com.system.common.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "processed_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(length = 64)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(length = 100)
    private String consumerGroup;

    @Builder.Default
    @Column(nullable = false)
    private Instant processedAt = Instant.now();
}
