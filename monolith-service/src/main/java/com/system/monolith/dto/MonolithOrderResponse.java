package com.system.monolith.dto;

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
public class MonolithOrderResponse {
    private String orderId;
    private String customerId;
    private String productId;
    private int quantity;
    private BigDecimal totalAmount;
    private String status;
    private String paymentId;
    private long executionDurationMs;
    private String message;
    private Instant timestamp;
}
