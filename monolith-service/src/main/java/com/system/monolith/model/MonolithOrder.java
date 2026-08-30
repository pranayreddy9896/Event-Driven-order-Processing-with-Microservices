package com.system.monolith.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monolith_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonolithOrder {

    @Id
    @Column(length = 36)
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String status; // CONFIRMED, FAILED

    @Builder.Default
    private Instant createdAt = Instant.now();
}
