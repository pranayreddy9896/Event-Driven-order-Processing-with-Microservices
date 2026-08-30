package com.system.order.dto;

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
public class SyncOrderResponse {
    private String orderId;
    private String status;
    private BigDecimal totalAmount;
    private long totalDurationMs;
    private long paymentServiceDurationMs;
    private String paymentStatus;
    private String message;
    private Instant timestamp;
}
