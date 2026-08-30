package com.system.inventory.controller;

import com.system.inventory.model.ProductStock;
import com.system.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<ProductStock>> getAllProducts() {
        return ResponseEntity.ok(inventoryService.getAllProducts());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductStock> getProduct(@PathVariable String productId) {
        return ResponseEntity.ok(inventoryService.getProduct(productId));
    }

    @PostMapping("/seed")
    public ResponseEntity<String> seedStock() {
        inventoryService.seedProducts();
        return ResponseEntity.ok("Inventory reseeded with test stock");
    }
}
