package com.system.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessRequest {
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private long simulateDelayMs;
    private boolean forceFailure;
}
