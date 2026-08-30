package com.system.order.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.common.model.OrderStatus;
import com.system.common.model.Topics;
import com.system.common.tracing.TraceContextHelper;
import com.system.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private final TraceContextHelper traceContextHelper;

    @KafkaListener(topics = Topics.INVENTORY_RESERVED, groupId = "order-saga-group")
    public void handleInventoryReserved(ConsumerRecord<String, String> record) {
        String traceId = traceContextHelper.extractTraceId(record);
        traceContextHelper.setLoggingContext(traceId);
        try {
            JsonNode root = objectMapper.readTree(record.value());
            String orderId = root.path("payload").path("orderId").asText();
            orderService.updateOrderStatus(orderId, OrderStatus.INVENTORY_RESERVED, null, null);
        } catch (Exception e) {
            log.error("Error processing inventory.reserved in order-service: {}", e.getMessage());
        } finally {
            traceContextHelper.clearLoggingContext();
        }
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = "order-saga-group")
    public void handlePaymentCompleted(ConsumerRecord<String, String> record) {
        String traceId = traceContextHelper.extractTraceId(record);
        traceContextHelper.setLoggingContext(traceId);
        try {
            JsonNode root = objectMapper.readTree(record.value());
            JsonNode payload = root.path("payload");
            String orderId = payload.path("orderId").asText();
            String paymentId = payload.path("paymentId").asText();
            orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED, paymentId, null);
            log.info("SAGA COMPLETED SUCCESS: Order {} confirmed with payment {}", orderId, paymentId);
        } catch (Exception e) {
            log.error("Error processing payment.completed in order-service: {}", e.getMessage());
        } finally {
            traceContextHelper.clearLoggingContext();
        }
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = "order-saga-group")
    public void handlePaymentFailed(ConsumerRecord<String, String> record) {
        String traceId = traceContextHelper.extractTraceId(record);
        traceContextHelper.setLoggingContext(traceId);
        try {
            JsonNode root = objectMapper.readTree(record.value());
            JsonNode payload = root.path("payload");
            String orderId = payload.path("orderId").asText();
            String reason = payload.path("failureReason").asText("Payment failed");
            orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED, null, reason);
            log.warn("SAGA FAILED & CANCELLED: Order {} cancelled due to payment failure: {}", orderId, reason);
        } catch (Exception e) {
            log.error("Error processing payment.failed in order-service: {}", e.getMessage());
        } finally {
            traceContextHelper.clearLoggingContext();
        }
    }

    @KafkaListener(topics = Topics.INVENTORY_FAILED, groupId = "order-saga-group")
    public void handleInventoryFailed(ConsumerRecord<String, String> record) {
        String traceId = traceContextHelper.extractTraceId(record);
        traceContextHelper.setLoggingContext(traceId);
        try {
            JsonNode root = objectMapper.readTree(record.value());
            JsonNode payload = root.path("payload");
            String orderId = payload.path("orderId").asText();
            String reason = payload.path("reason").asText("Out of stock");
            orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED, null, reason);
            log.warn("SAGA FAILED & CANCELLED: Order {} cancelled due to inventory shortage: {}", orderId, reason);
        } catch (Exception e) {
            log.error("Error processing inventory.failed in order-service: {}", e.getMessage());
        } finally {
            traceContextHelper.clearLoggingContext();
        }
    }
}
