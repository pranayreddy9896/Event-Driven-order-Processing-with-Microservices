package com.system.inventory.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservation {

    @Id
    @Column(length = 36)
    @Builder.Default
    private String reservationId = UUID.randomUUID().toString();

    @Column(nullable = false, length = 64)
    private String orderId;

    @Column(nullable = false, length = 64)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.RESERVED;

    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant releasedAt;

    private Instant confirmedAt;
}
