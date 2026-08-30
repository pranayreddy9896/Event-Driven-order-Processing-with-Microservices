package com.system.inventory.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.common.model.Topics;
import com.system.common.tracing.TraceContextHelper;
import com.system.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    private final TraceContextHelper traceContextHelper;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = Topics.ORDER_CREATED, groupId = "inventory-service-group")
    public void handleOrderCreated(ConsumerRecord<String, String> record) {
        String traceId = traceContextHelper.extractTraceId(record);
        traceContextHelper.setLoggingContext(traceId);

        try {
            log.info("Inventory consumer received [order.created] on topic [{}] offset [{}]: {}",
                    record.topic(), record.offset(), record.value());

            JsonNode root = objectMapper.readTree(record.value());
            String eventId = root.path("eventId").asText();
            JsonNode payload = root.path("payload");

            String orderId = payload.path("orderId").asText();
            String productId = payload.path("productId").asText();
            int quantity = payload.path("quantity").asInt();

            inventoryService.reserveStock(eventId, orderId, productId, quantity, traceId);
        } catch (Exception e) {
            log.error("Error processing order.created in inventory-service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed processing inventory reservation: " + record.key(), e);
        } finally {
            traceContextHelper.clearLoggingContext();
        }
    }

    /**
     * SAGA COMPENSATION LISTENER:
     * Listens to payment.failed event and reverts the stock reservation.
     */
    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = "inventory-compensation-group")
    public void handlePaymentFailedCompensatingTransaction(ConsumerRecord<String, String> record) {
        String traceId = traceContextHelper.extractTraceId(record);
        traceContextHelper.setLoggingContext(traceId);

        try {
            log.warn("Inventory consumer received SAGA COMPENSATION [payment.failed] on topic [{}] offset [{}]: {}",
                    record.topic(), record.offset(), record.value());

            JsonNode root = objectMapper.readTree(record.value());
            String eventId = root.path("eventId").asText();
            JsonNode payload = root.path("payload");

            String orderId = payload.path("orderId").asText();

            inventoryService.compensateReleaseStock(eventId, orderId, traceId);
        } catch (Exception e) {
            log.error("Error executing saga compensation in inventory-service: {}", e.getMessage(), e);
        } finally {
            traceContextHelper.clearLoggingContext();
        }
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = "inventory-confirmation-group")
    public void handlePaymentCompleted(ConsumerRecord<String, String> record) {
        try {
            JsonNode root = objectMapper.readTree(record.value());
            JsonNode payload = root.path("payload");
            String orderId = payload.path("orderId").asText();
            inventoryService.confirmReservation(orderId);
        } catch (Exception e) {
            log.error("Error confirming inventory reservation: {}", e.getMessage());
        }
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record) {
        log.error("DEAD LETTER TOPIC: Inventory poisoned message routed to DLT from topic [{}] key [{}] payload [{}]",
                record.topic(), record.key(), record.value());
    }
}
