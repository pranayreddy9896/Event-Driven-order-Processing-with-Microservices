package com.system.notification.service;

import com.system.common.idempotency.IdempotencyHandler;
import com.system.notification.model.NotificationLog;
import com.system.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository notificationRepository;
    private final IdempotencyHandler idempotencyHandler;

    @Transactional
    public void sendOrderConfirmation(String eventId, String orderId, String customerId, String paymentId) {
        if (idempotencyHandler.isAlreadyProcessed(eventId)) {
            log.warn("IDEMPOTENCY GUARD: Event {} already processed. Skipping duplicate email for order {}", eventId, orderId);
            return;
        }

        String recipient = customerId + "@example.com";
        String subject = "Your Order " + orderId + " has been Confirmed!";
        String message = String.format("Dear %s, your payment (ID: %s) was successful. Your order %s is now being prepared for shipping.",
                customerId, paymentId, orderId);

        NotificationLog record = NotificationLog.builder()
                .orderId(orderId)
                .recipient(recipient)
                .channel("EMAIL")
                .subject(subject)
                .message(message)
                .status("SENT")
                .sentAt(Instant.now())
                .build();
        notificationRepository.save(record);

        log.info("DISPATCHED EMAIL to {} for confirmed order {}: '{}'", recipient, orderId, subject);

        idempotencyHandler.markAsProcessed(eventId, "PaymentCompletedNotification", "notification-service-group");
    }

    @Transactional
    public void sendOrderCancellationAlert(String eventId, String orderId, String customerId, String reason) {
        if (idempotencyHandler.isAlreadyProcessed(eventId)) {
            log.warn("IDEMPOTENCY GUARD: Event {} already processed. Skipping duplicate alert for order {}", eventId, orderId);
            return;
        }

        String recipient = customerId + "@example.com";
        String subject = "Order " + orderId + " Failed - Action Required";
        String message = String.format("Dear %s, your order %s could not be completed due to: %s. Any temporary holds have been released.",
                customerId, orderId, reason);

        NotificationLog record = NotificationLog.builder()
                .orderId(orderId)
                .recipient(recipient)
                .channel("EMAIL")
                .subject(subject)
                .message(message)
                .status("SENT")
                .sentAt(Instant.now())
                .build();
        notificationRepository.save(record);

        log.warn("DISPATCHED CANCELLATION EMAIL to {} for failed order {}: '{}'", recipient, orderId, subject);

        idempotencyHandler.markAsProcessed(eventId, "OrderCancelledNotification", "notification-service-group");
    }

    public List<NotificationLog> getAllNotifications() {
        return notificationRepository.findAllByOrderBySentAtDesc();
    }

    public List<NotificationLog> getNotificationsByOrderId(String orderId) {
        return notificationRepository.findByOrderId(orderId);
    }
}
