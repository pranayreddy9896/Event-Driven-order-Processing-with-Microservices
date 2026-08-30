package com.system.payment.dto;

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
public class PaymentProcessResponse {
    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private String status; // SUCCESS, FAILED
    private String transactionRef;
    private String failureReason;
    private long executionDurationMs;
    private Instant processedAt;
}
