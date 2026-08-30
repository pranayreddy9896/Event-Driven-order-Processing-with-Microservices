package com.system.monolith.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "monolith_products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonolithProduct {

    @Id
    private String productId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private int availableStock;

    @Version
    private Long version;
}
