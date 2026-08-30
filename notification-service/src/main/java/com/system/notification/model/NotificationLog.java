package com.system.notification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    @Id
    @Column(length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, length = 64)
    private String orderId;

    @Column(nullable = false, length = 100)
    private String recipient;

    @Column(nullable = false, length = 20)
    private String channel; // EMAIL, SMS

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 20)
    private String status; // SENT, FAILED

    @Builder.Default
    @Column(nullable = false)
    private Instant sentAt = Instant.now();
}
