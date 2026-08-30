package com.system.order.service;

import com.system.common.events.OrderCreatedEvent;
import com.system.common.model.OrderStatus;
import com.system.common.model.Topics;
import com.system.common.outbox.OutboxPublisher;
import com.system.common.tracing.TraceContextHelper;
import com.system.order.dto.CreateOrderRequest;
import com.system.order.dto.OrderResponse;
import com.system.order.dto.SyncOrderResponse;
import com.system.order.model.Order;
import com.system.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxPublisher outboxPublisher;
    private final TraceContextHelper traceContextHelper;
    private final RestTemplate restTemplate;

    @Value("${services.payment.url:http://localhost:8082}")
    private String paymentServiceUrl;

    /**
     * STEP 3-8: Asynchronous Event-Driven Order Creation.
     * Writes Order row and Outbox event in a single atomic DB transaction.
     * Returns immediately (Eventual Consistency).
     */
    @Transactional
    public OrderResponse createOrderAsync(CreateOrderRequest request) {
        String traceId = traceContextHelper.getCurrentTraceId();
        traceContextHelper.setLoggingContext(traceId);

        try {
            BigDecimal unitPrice = request.getUnitPrice() != null ? request.getUnitPrice() : new BigDecimal("1999.99");
            BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

            String orderId = UUID.randomUUID().toString();

            Order order = Order.builder()
                    .id(orderId)
                    .customerId(request.getCustomerId())
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .totalAmount(totalAmount)
                    .status(OrderStatus.PENDING)
                    .traceId(traceId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            orderRepository.save(order);

            // ATOMIC TRANSACTIONAL OUTBOX WRITE:
            // Write event to outbox_events table in the exact same DB transaction!
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(orderId)
                    .customerId(request.getCustomerId())
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .totalAmount(totalAmount)
                    .build();

            outboxPublisher.enqueue(
                    Topics.ORDER_CREATED,
                    "OrderCreatedEvent",
                    "Order",
                    orderId,
                    traceId,
                    event
            );

            log.info("ASYNC ORDER ACCEPTED: Order {} created in PENDING status. Event enqueued to Outbox. TraceId={}",
                    orderId, traceId);

            return mapToResponse(order, "Order accepted. Processing asynchronously via Kafka event stream.");
        } finally {
            traceContextHelper.clearLoggingContext();
        }
    }

    /**
     * STEP 2: Synchronous Microservice Call.
     * Demonstrates blocking I/O and latency degradation when downstream Payment service is slow.
     */
    public SyncOrderResponse createOrderSync(CreateOrderRequest request, long simulateDelayMs, boolean forceFail) {
        long startTime = System.currentTimeMillis();
        String orderId = UUID.randomUUID().toString();
        BigDecimal unitPrice = request.getUnitPrice() != null ? request.getUnitPrice() : new BigDecimal("1999.99");
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        log.warn("STEP 2 DEMO: Calling Payment Service synchronously at {}/api/payment/process (delay={}ms)",
                paymentServiceUrl, simulateDelayMs);

        Map<String, Object> paymentPayload = Map.of(
                "orderId", orderId,
                "customerId", request.getCustomerId(),
                "amount", totalAmount,
                "simulateDelayMs", simulateDelayMs,
                "forceFailure", forceFail
        );

        long paymentStart = System.currentTimeMillis();
        String paymentStatus = "FAILED";
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    paymentServiceUrl + "/api/payment/process",
                    paymentPayload,
                    Map.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                paymentStatus = (String) response.getBody().get("status");
            }
        } catch (Exception e) {
            log.error("Synchronous call to payment-service failed: {}", e.getMessage());
            paymentStatus = "FAILED_HTTP_ERROR: " + e.getMessage();
        }
        long paymentDuration = System.currentTimeMillis() - paymentStart;
        long totalDuration = System.currentTimeMillis() - startTime;

        OrderStatus finalStatus = "SUCCESS".equalsIgnoreCase(paymentStatus) ? OrderStatus.CONFIRMED : OrderStatus.FAILED;

        Order order = Order.builder()
                .id(orderId)
                .customerId(request.getCustomerId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalAmount(totalAmount)
                .status(finalStatus)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        orderRepository.save(order);

        return SyncOrderResponse.builder()
                .orderId(orderId)
                .status(finalStatus.name())
                .totalAmount(totalAmount)
                .totalDurationMs(totalDuration)
                .paymentServiceDurationMs(paymentDuration)
                .paymentStatus(paymentStatus)
                .message("Synchronous HTTP execution completed. Notice total response latency includes downstream delay!")
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Updates order state during Saga orchestration/choreography.
     */
    @Transactional
    public void updateOrderStatus(String orderId, OrderStatus newStatus, String paymentId, String failureReason) {
        orderRepository.findById(orderId).ifPresentOrElse(order -> {
            order.setStatus(newStatus);
            if (paymentId != null) {
                order.setPaymentId(paymentId);
            }
            if (failureReason != null) {
                order.setFailureReason(failureReason);
            }
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);
            log.info("Order {} transitioned status to {} (paymentId={}, reason={})",
                    orderId, newStatus, paymentId, failureReason);
        }, () -> log.error("Order not found for status update: {}", orderId));
    }

    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return mapToResponse(order, "Order retrieved successfully");
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    private OrderResponse mapToResponse(Order order, String message) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentId(order.getPaymentId())
                .failureReason(order.getFailureReason())
                .traceId(order.getTraceId())
                .message(message)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
