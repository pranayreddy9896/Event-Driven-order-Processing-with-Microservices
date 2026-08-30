package com.system.inventory.service;

import com.system.common.events.InventoryFailedEvent;
import com.system.common.events.InventoryReservedEvent;
import com.system.common.idempotency.IdempotencyHandler;
import com.system.common.model.Topics;
import com.system.common.outbox.OutboxPublisher;
import com.system.inventory.model.InventoryReservation;
import com.system.inventory.model.ProductStock;
import com.system.inventory.model.ReservationStatus;
import com.system.inventory.repository.InventoryReservationRepository;
import com.system.inventory.repository.ProductStockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductStockRepository productStockRepository;
    private final InventoryReservationRepository reservationRepository;
    private final OutboxPublisher outboxPublisher;
    private final IdempotencyHandler idempotencyHandler;

    @PostConstruct
    public void initProducts() {
        seedProducts();
    }

    public void seedProducts() {
        productStockRepository.save(ProductStock.builder()
                .productId("PROD-1")
                .productName("MacBook Pro M3 Max")
                .unitPrice(new BigDecimal("1999.99"))
                .availableQuantity(100)
                .reservedQuantity(0)
                .build());

        productStockRepository.save(ProductStock.builder()
                .productId("PROD-2")
                .productName("Keychron Mechanical Keyboard")
                .unitPrice(new BigDecimal("99.99"))
                .availableQuantity(2) // Low stock to test out-of-stock failure
                .reservedQuantity(0)
                .build());

        productStockRepository.save(ProductStock.builder()
                .productId("PROD-3")
                .productName("Logitech MX Master 3S")
                .unitPrice(new BigDecimal("79.99"))
                .availableQuantity(50)
                .reservedQuantity(0)
                .build());

        log.info("Initialized Inventory stock seed data");
    }

    /**
     * Reserve stock upon receiving order.created event.
     */
    @Transactional
    public void reserveStock(String eventId, String orderId, String productId, int quantity, String traceId) {
        log.info("Attempting to reserve stock for orderId={}, product={}, qty={}, traceId={}",
                orderId, productId, quantity, traceId);

        if (idempotencyHandler.isAlreadyProcessed(eventId)) {
            log.warn("IDEMPOTENCY GUARD: Event {} already processed. Skipping duplicate reservation for order {}",
                    eventId, orderId);
            return;
        }

        ProductStock product = productStockRepository.findById(productId).orElse(null);

        if (product == null || product.getAvailableQuantity() < quantity) {
            int available = product != null ? product.getAvailableQuantity() : 0;
            log.warn("INVENTORY FAILURE: Insufficient stock for product {}. Available={}, Requested={}",
                    productId, available, quantity);

            InventoryFailedEvent failedEvent = InventoryFailedEvent.builder()
                    .orderId(orderId)
                    .productId(productId)
                    .requestedQuantity(quantity)
                    .availableQuantity(available)
                    .reason("Insufficient inventory in warehouse")
                    .build();

            outboxPublisher.enqueue(
                    Topics.INVENTORY_FAILED,
                    "InventoryFailedEvent",
                    "Inventory",
                    orderId,
                    traceId,
                    failedEvent
            );
        } else {
            // Reserve stock
            product.setAvailableQuantity(product.getAvailableQuantity() - quantity);
            product.setReservedQuantity(product.getReservedQuantity() + quantity);
            productStockRepository.save(product);

            InventoryReservation reservation = InventoryReservation.builder()
                    .orderId(orderId)
                    .productId(productId)
                    .quantity(quantity)
                    .status(ReservationStatus.RESERVED)
                    .createdAt(Instant.now())
                    .build();
            reservationRepository.save(reservation);

            BigDecimal totalAmount = product.getUnitPrice().multiply(BigDecimal.valueOf(quantity));

            InventoryReservedEvent reservedEvent = InventoryReservedEvent.builder()
                    .orderId(orderId)
                    .productId(productId)
                    .quantity(quantity)
                    .totalAmount(totalAmount)
                    .reservationId(reservation.getReservationId())
                    .build();

            outboxPublisher.enqueue(
                    Topics.INVENTORY_RESERVED,
                    "InventoryReservedEvent",
                    "Inventory",
                    orderId,
                    traceId,
                    reservedEvent
            );

            log.info("INVENTORY RESERVED: Reserved {} units of {} for order {}. Remaining available={}",
                    quantity, productId, orderId, product.getAvailableQuantity());
        }

        idempotencyHandler.markAsProcessed(eventId, "OrderCreatedEvent", "inventory-service-group");
    }

    /**
     * SAGA COMPENSATING TRANSACTION:
     * Triggered when Payment fails after stock was reserved.
     * Restores the reserved quantity back to available inventory.
     */
    @Transactional
    public void compensateReleaseStock(String eventId, String orderId, String traceId) {
        log.warn("SAGA COMPENSATION TRIGGERED: Releasing reserved stock for orderId={}, traceId={}",
                orderId, traceId);

        if (idempotencyHandler.isAlreadyProcessed(eventId)) {
            log.warn("IDEMPOTENCY GUARD: Compensation event {} already processed. Skipping for order {}", eventId, orderId);
            return;
        }

        Optional<InventoryReservation> optReservation = reservationRepository.findFirstByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);

        if (optReservation.isPresent()) {
            InventoryReservation reservation = optReservation.get();
            ProductStock product = productStockRepository.findById(reservation.getProductId()).orElse(null);

            if (product != null) {
                // Compensating action: revert reservation
                product.setReservedQuantity(Math.max(0, product.getReservedQuantity() - reservation.getQuantity()));
                product.setAvailableQuantity(product.getAvailableQuantity() + reservation.getQuantity());
                productStockRepository.save(product);

                reservation.setStatus(ReservationStatus.RELEASED);
                reservation.setReleasedAt(Instant.now());
                reservationRepository.save(reservation);

                log.warn("SAGA COMPENSATION SUCCESS: Stock refunded! Product {}, qty {} restored to available={}",
                        product.getProductId(), reservation.getQuantity(), product.getAvailableQuantity());
            }
        } else {
            log.info("No active RESERVED reservation found for order {}. Compensation skipped or already released.", orderId);
        }

        idempotencyHandler.markAsProcessed(eventId, "PaymentFailedCompensation", "inventory-service-group");
    }

    /**
     * Confirm reservation on successful payment.
     */
    @Transactional
    public void confirmReservation(String orderId) {
        Optional<InventoryReservation> optReservation = reservationRepository.findFirstByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);
        if (optReservation.isPresent()) {
            InventoryReservation reservation = optReservation.get();
            ProductStock product = productStockRepository.findById(reservation.getProductId()).orElse(null);

            if (product != null) {
                product.setReservedQuantity(Math.max(0, product.getReservedQuantity() - reservation.getQuantity()));
                productStockRepository.save(product);
            }

            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation.setConfirmedAt(Instant.now());
            reservationRepository.save(reservation);

            log.info("Inventory reservation CONFIRMED for orderId={}", orderId);
        }
    }

    public List<ProductStock> getAllProducts() {
        return productStockRepository.findAll();
    }

    public ProductStock getProduct(String productId) {
        return productStockRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }
}
