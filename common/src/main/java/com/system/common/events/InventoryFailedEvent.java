package com.system.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryFailedEvent {
    private String orderId;
    private String productId;
    private int requestedQuantity;
    private int availableQuantity;
    private String reason;
}
