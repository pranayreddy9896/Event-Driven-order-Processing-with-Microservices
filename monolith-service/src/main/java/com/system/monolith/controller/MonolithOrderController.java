package com.system.monolith.controller;

import com.system.monolith.dto.CreateOrderRequest;
import com.system.monolith.dto.MonolithOrderResponse;
import com.system.monolith.model.MonolithOrder;
import com.system.monolith.model.MonolithProduct;
import com.system.monolith.service.MonolithOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monolith")
@RequiredArgsConstructor
public class MonolithOrderController {

    private final MonolithOrderService orderService;

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            MonolithOrderResponse response = orderService.processOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "FAILED",
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<MonolithOrder> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @GetMapping("/inventory")
    public ResponseEntity<List<MonolithProduct>> getInventory() {
        return ResponseEntity.ok(orderService.getAllProducts());
    }

    @PostMapping("/inventory/seed")
    public ResponseEntity<String> reseed() {
        orderService.seedProducts();
        return ResponseEntity.ok("Inventory reseeded successfully");
    }
}
