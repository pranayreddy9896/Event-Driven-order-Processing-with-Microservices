package com.system.notification.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.common.model.Topics;
import com.system.common.tracing.TraceContextHelper;
import com.system.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final TraceContextHelper traceContextHelper;

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = "notification-service-group")
    public void handlePaymentCompleted(ConsumerRecord<String, String> record) {
        String traceId = traceContextHelper.extractTraceId(record);
        traceContextHelper.setLoggingContext(traceId);
        try {
            JsonNode root = objectMapper.readTree(record.value());
            String eventId = root.path("eventId").asText();
            JsonNode payload = root.path("payload");

            String orderId = payload.path("orderId").asText();
            String customerId = payload.path("customerId").asText("customer");
            String paymentId = payload.path("paymentId").asText("N/A");

            notificationService.sendOrderConfirmation(eventId, orderId, customerId, paymentId);
        } catch (Exception e) {
            log.error("Error processing payment.completed notification: {}", e.getMessage());
        } finally {
            traceContextHelper.clearLoggingContext();
        }
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = "notification-service-group")
    public void handlePaymentFailed(ConsumerRecord<String, String> record) {
        String traceId = traceContextHelper.extractTraceId(record);
        traceContextHelper.setLoggingContext(traceId);
        try {
            JsonNode root = objectMapper.readTree(record.value());
            String eventId = root.path("eventId").asText();
            JsonNode payload = root.path("payload");

            String orderId = payload.path("orderId").asText();
            String customerId = payload.path("customerId").asText("customer");
            String reason = payload.path("failureReason").asText("Payment was declined");

            notificationService.sendOrderCancellationAlert(eventId, orderId, customerId, reason);
        } catch (Exception e) {
            log.error("Error processing payment.failed notification: {}", e.getMessage());
        } finally {
            traceContextHelper.clearLoggingContext();
        }
    }

    @KafkaListener(topics = Topics.INVENTORY_FAILED, groupId = "notification-service-group")
    public void handleInventoryFailed(ConsumerRecord<String, String> record) {
        String traceId = traceContextHelper.extractTraceId(record);
        traceContextHelper.setLoggingContext(traceId);
        try {
            JsonNode root = objectMapper.readTree(record.value());
            String eventId = root.path("eventId").asText();
            JsonNode payload = root.path("payload");

            String orderId = payload.path("orderId").asText();
            String customerId = "customer";
            String reason = payload.path("reason").asText("Item out of stock");

            notificationService.sendOrderCancellationAlert(eventId, orderId, customerId, reason);
        } catch (Exception e) {
            log.error("Error processing inventory.failed notification: {}", e.getMessage());
        } finally {
            traceContextHelper.clearLoggingContext();
        }
    }
}
