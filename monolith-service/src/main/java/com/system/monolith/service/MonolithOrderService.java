package com.system.monolith.service;

import com.system.monolith.dto.CreateOrderRequest;
import com.system.monolith.dto.MonolithOrderResponse;
import com.system.monolith.model.MonolithOrder;
import com.system.monolith.model.MonolithPayment;
import com.system.monolith.model.MonolithProduct;
import com.system.monolith.repository.MonolithOrderRepository;
import com.system.monolith.repository.MonolithPaymentRepository;
import com.system.monolith.repository.MonolithProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonolithOrderService {

    private final MonolithOrderRepository orderRepository;
    private final MonolithProductRepository productRepository;
    private final MonolithPaymentRepository paymentRepository;

    @PostConstruct
    public void initSeedData() {
        seedProducts();
    }

    public void seedProducts() {
        productRepository.save(MonolithProduct.builder()
                .productId("PROD-1")
                .name("MacBook Pro M3")
                .price(new BigDecimal("1999.99"))
                .availableStock(100)
                .build());

        productRepository.save(MonolithProduct.builder()
                .productId("PROD-2")
                .name("Keychron K2 Mechanical Keyboard")
                .price(new BigDecimal("99.99"))
                .availableStock(2)
                .build());

        log.info("Initialized Monolith seed products");
    }

    /**
     * Executes order creation in a single ACID database transaction.
     * If payment fails, Spring rolls back the entire transaction, restoring the stock quantity automatically.
     */
    @Transactional
    public MonolithOrderResponse processOrder(CreateOrderRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Starting Monolith Order processing for customer={}, product={}, qty={}",
                request.getCustomerId(), request.getProductId(), request.getQuantity());

        // 1. Inventory Check & Reservation
        MonolithProduct product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getProductId()));

        if (product.getAvailableStock() < request.getQuantity()) {
            throw new IllegalStateException("Insufficient stock! Available: " + product.getAvailableStock() + ", Requested: " + request.getQuantity());
        }

        // Deduct stock in DB
        product.setAvailableStock(product.getAvailableStock() - request.getQuantity());
        productRepository.save(product);
        log.info("Deducted stock for {}. Remaining stock: {}", product.getProductId(), product.getAvailableStock());

        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        String orderId = UUID.randomUUID().toString();

        // 2. Simulate Payment Delay if configured
        if (request.getSimulatePaymentDelayMs() > 0) {
            try {
                log.info("Simulating payment gateway latency: {}ms", request.getSimulatePaymentDelayMs());
                Thread.sleep(request.getSimulatePaymentDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 3. Payment Processing
        if (request.isSimulatePaymentFailure()) {
            log.error("Payment failed simulated for orderId={}. Triggering ACID rollback!", orderId);
            // This runtime exception causes the whole transaction to rollback automatically!
            throw new RuntimeException("Payment processing failed for orderId=" + orderId + ". ACID rollback triggered!");
        }

        MonolithPayment payment = MonolithPayment.builder()
                .orderId(orderId)
                .customerId(request.getCustomerId())
                .amount(totalAmount)
                .status("SUCCESS")
                .processedAt(Instant.now())
                .build();
        paymentRepository.save(payment);

        // 4. Save Confirmed Order
        MonolithOrder order = MonolithOrder.builder()
                .id(orderId)
                .customerId(request.getCustomerId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalAmount(totalAmount)
                .status("CONFIRMED")
                .createdAt(Instant.now())
                .build();
        orderRepository.save(order);

        // 5. Send Notification (Synchronous Mock)
        log.info("NOTIFICATION SENT: Order {} confirmed for customer {}", orderId, request.getCustomerId());

        long duration = System.currentTimeMillis() - startTime;
        log.info("Monolith Order {} processed successfully in {}ms", orderId, duration);

        return MonolithOrderResponse.builder()
                .orderId(orderId)
                .customerId(request.getCustomerId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .totalAmount(totalAmount)
                .status("CONFIRMED")
                .paymentId(payment.getId())
                .executionDurationMs(duration)
                .message("Order processed atomically within a single ACID transaction")
                .timestamp(Instant.now())
                .build();
    }

    public List<MonolithProduct> getAllProducts() {
        return productRepository.findAll();
    }

    public MonolithOrder getOrder(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }
}
