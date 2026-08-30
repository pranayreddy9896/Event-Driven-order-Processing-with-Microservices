package com.system.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @Column(length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, length = 64)
    private String orderId;

    @Column(nullable = false, length = 64)
    private String customerId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS, FAILED

    @Column(length = 64)
    private String transactionRef;

    @Column(length = 255)
    private String failureReason;

    @Builder.Default
    @Column(nullable = false)
    private Instant processedAt = Instant.now();
}
