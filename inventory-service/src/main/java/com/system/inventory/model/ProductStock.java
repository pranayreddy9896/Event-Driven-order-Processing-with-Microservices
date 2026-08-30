package com.system.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products_stock")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStock {

    @Id
    @Column(length = 64)
    private String productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    @Builder.Default
    private int reservedQuantity = 0;

    @Version
    private Long version;
}
