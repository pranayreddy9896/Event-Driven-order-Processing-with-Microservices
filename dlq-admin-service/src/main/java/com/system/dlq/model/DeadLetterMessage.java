package com.system.dlq.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dead_letter_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterMessage {

    @Id
    @Column(length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, length = 100)
    private String originalTopic;

    @Column(nullable = false, length = 100)
    private String dltTopic;

    @Column(length = 100)
    private String messageKey;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String exceptionMessage;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String headers;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "POISONED"; // POISONED, REPLAYED, DISCARDED

    @Builder.Default
    @Column(nullable = false)
    private Instant receivedAt = Instant.now();

    private Instant replayedAt;
}
