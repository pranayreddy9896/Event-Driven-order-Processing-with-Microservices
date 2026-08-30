package com.system.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedEvent {
    private String orderId;
    private String productId;
    private int quantity;
    private BigDecimal totalAmount;
    private String reservationId;
}
