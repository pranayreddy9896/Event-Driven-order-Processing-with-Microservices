package com.system.order.dto;

import com.system.common.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String orderId;
    private String customerId;
    private String productId;
    private int quantity;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String paymentId;
    private String failureReason;
    private String traceId;
    private String message;
    private Instant createdAt;
    private Instant updatedAt;
}
