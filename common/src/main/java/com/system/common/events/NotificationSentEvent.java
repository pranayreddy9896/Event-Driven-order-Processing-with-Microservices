package com.system.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSentEvent {
    private String notificationId;
    private String recipient;
    private String channel; // EMAIL, SMS
    private String subject;
    private String message;
    private Instant sentAt;
}
