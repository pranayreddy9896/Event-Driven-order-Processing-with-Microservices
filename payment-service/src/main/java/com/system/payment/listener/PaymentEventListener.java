package com.system.payment.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.common.model.Topics;
import com.system.common.tracing.TraceContextHelper;
import com.system.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    private final TraceContextHelper traceContextHelper;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = Topics.INVENTORY_RESERVED, groupId = "payment-service-group")
    public void handleInventoryReserved(ConsumerRecord<String, String> record) {
        String traceId = traceContextHelper.extractTraceId(record);
        traceContextHelper.setLoggingContext(traceId);

        try {
            log.info("Kafka consumer received event on topic [{}] offset [{}]: {}",
                    record.topic(), record.offset(), record.value());

            JsonNode root = objectMapper.readTree(record.value());
            String eventId = root.path("eventId").asText();
            JsonNode payload = root.path("payload");

            String orderId = payload.path("orderId").asText();
            BigDecimal totalAmount = new BigDecimal(payload.path("totalAmount").asText("0.00"));
            String customerId = payload.has("customerId") ? payload.path("customerId").asText() : "CUST-DEFAULT";

            paymentService.processAsyncPaymentFromEvent(eventId, orderId, customerId, totalAmount, traceId);
        } catch (Exception e) {
            log.error("Error processing inventory.reserved in payment-service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed processing payment for message: " + record.key(), e);
        } finally {
            traceContextHelper.clearLoggingContext();
        }
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record) {
        log.error("DEAD LETTER TOPIC HANDLER: Poisoned message routed to DLT from topic [{}] key [{}] payload [{}]",
                record.topic(), record.key(), record.value());
    }
}
