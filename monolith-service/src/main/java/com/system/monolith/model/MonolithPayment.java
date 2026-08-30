package com.system.monolith.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monolith_payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonolithPayment {

    @Id
    @Column(length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status; // SUCCESS, FAILED

    @Builder.Default
    private Instant processedAt = Instant.now();
}
