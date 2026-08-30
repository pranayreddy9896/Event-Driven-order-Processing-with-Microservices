package com.system.order.controller;

import com.system.order.dto.CreateOrderRequest;
import com.system.order.dto.OrderResponse;
import com.system.order.dto.SyncOrderResponse;
import com.system.order.model.Order;
import com.system.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Step 3-8: Asynchronous Event-Driven Order Creation.
     * Returns 202 Accepted immediately.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrderAsync(@RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrderAsync(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Step 2: Synchronous Microservice call with simulated delay and failure injection.
     */
    @PostMapping("/sync")
    public ResponseEntity<SyncOrderResponse> createOrderSync(
            @RequestBody CreateOrderRequest request,
            @RequestParam(defaultValue = "0") long delayMs,
            @RequestParam(defaultValue = "false") boolean forceFail) {
        SyncOrderResponse response = orderService.createOrderSync(request, delayMs, forceFail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
}
