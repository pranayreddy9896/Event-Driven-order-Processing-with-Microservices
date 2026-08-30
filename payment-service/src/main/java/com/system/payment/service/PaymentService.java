package com.system.payment.service;

import com.system.common.events.PaymentCompletedEvent;
import com.system.common.events.PaymentFailedEvent;
import com.system.common.idempotency.IdempotencyHandler;
import com.system.common.model.Topics;
import com.system.common.outbox.OutboxPublisher;
import com.system.payment.config.PaymentSimulationConfig;
import com.system.payment.dto.PaymentProcessRequest;
import com.system.payment.dto.PaymentProcessResponse;
import com.system.payment.model.Payment;
import com.system.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxPublisher outboxPublisher;
    private final IdempotencyHandler idempotencyHandler;
    private final PaymentSimulationConfig simulationConfig;

    /**
     * Synchronous HTTP payment processing (Step 2 demonstration).
     */
    @Transactional
    public PaymentProcessResponse processSyncPayment(PaymentProcessRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Processing synchronous payment for orderId={}, amount={}", request.getOrderId(), request.getAmount());

        // Apply artificial delay if specified in request or global config
        long delay = request.getSimulateDelayMs() > 0 ? request.getSimulateDelayMs() : simulationConfig.getDelayMs();
        if (delay > 0) {
            try {
                log.warn("Injecting artificial delay of {}ms into synchronous payment for order {}", delay, request.getOrderId());
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        boolean forceFail = request.isForceFailure() || simulationConfig.isForceFailure()
                || (simulationConfig.getFailureRate() > 0 && ThreadLocalRandom.current().nextInt(100) < simulationConfig.getFailureRate());

        if (forceFail) {
            Payment failedPayment = Payment.builder()
                    .orderId(request.getOrderId())
                    .customerId(request.getCustomerId())
                    .amount(request.getAmount())
                    .status("FAILED")
                    .failureReason("Payment rejected by bank / simulated decline")
                    .processedAt(Instant.now())
                    .build();
            paymentRepository.save(failedPayment);

            long duration = System.currentTimeMillis() - startTime;
            log.error("Synchronous payment failed for orderId={} in {}ms", request.getOrderId(), duration);
            return PaymentProcessResponse.builder()
                    .paymentId(failedPayment.getId())
                    .orderId(request.getOrderId())
                    .amount(request.getAmount())
                    .status("FAILED")
                    .failureReason("Payment rejected by bank / simulated decline")
                    .executionDurationMs(duration)
                    .processedAt(Instant.now())
                    .build();
        }

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .status("SUCCESS")
                .transactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .processedAt(Instant.now())
                .build();
        paymentRepository.save(payment);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Synchronous payment SUCCESS for orderId={} [txnRef={}] in {}ms",
                request.getOrderId(), payment.getTransactionRef(), duration);

        return PaymentProcessResponse.builder()
                .paymentId(payment.getId())
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .status("SUCCESS")
                .transactionRef(payment.getTransactionRef())
                .executionDurationMs(duration)
                .processedAt(Instant.now())
                .build();
    }

    /**
     * Asynchronous payment processing triggered by Kafka InventoryReserved event (Step 3+).
     * Atomic Outbox write ensures DB update and Kafka event cannot diverge.
     */
    @Transactional
    public void processAsyncPaymentFromEvent(String eventId, String orderId, String customerId, BigDecimal amount, String traceId) {
        log.info("Received async payment request: eventId={}, orderId={}, amount={}, traceId={}",
                eventId, orderId, amount, traceId);

        // Check Idempotency: Prevent double charging if message was redelivered!
        if (idempotencyHandler.isAlreadyProcessed(eventId)) {
            log.warn("IDEMPOTENCY GUARD: Event {} already processed. Skipping duplicate payment processing for order {}",
                    eventId, orderId);
            return;
        }

        // Apply artificial delay if configured
        if (simulationConfig.getDelayMs() > 0) {
            try {
                log.info("Applying simulated delay: {}ms", simulationConfig.getDelayMs());
                Thread.sleep(simulationConfig.getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Simulate poison pill for DLQ testing
        if (simulationConfig.isForcePoisonPill()) {
            log.error("POISON PILL TRIGGERED! Simulating unrecoverable exception for DLQ demonstration.");
            throw new RuntimeException("Simulated Poison Pill Exception for DLQ Replay Testing! Order: " + orderId);
        }

        boolean forceFail = simulationConfig.isForceFailure()
                || (simulationConfig.getFailureRate() > 0 && ThreadLocalRandom.current().nextInt(100) < simulationConfig.getFailureRate());

        if (forceFail) {
            log.warn("Payment FAILED (simulated) for orderId={}. Writing payment.failed to Outbox for Saga Compensation.", orderId);
            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .customerId(customerId != null ? customerId : "CUST-UNKNOWN")
                    .amount(amount)
                    .status("FAILED")
                    .failureReason("Insufficient funds / Card declined (Simulated)")
                    .processedAt(Instant.now())
                    .build();
            paymentRepository.save(payment);

            // Write PaymentFailedEvent to Outbox (ATOMIC LOCAL TRANSACTION)
            PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                    .orderId(orderId)
                    .customerId(payment.getCustomerId())
                    .amount(amount)
                    .failureReason("Card declined / insufficient funds")
                    .build();

            outboxPublisher.enqueue(
                    Topics.PAYMENT_FAILED,
                    "PaymentFailedEvent",
                    "Payment",
                    orderId,
                    traceId,
                    failedEvent
            );
        } else {
            log.info("Payment SUCCESS for orderId={}. Writing payment.completed to Outbox.", orderId);
            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .customerId(customerId != null ? customerId : "CUST-UNKNOWN")
                    .amount(amount)
                    .status("SUCCESS")
                    .transactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .processedAt(Instant.now())
                    .build();
            paymentRepository.save(payment);

            // Write PaymentCompletedEvent to Outbox (ATOMIC LOCAL TRANSACTION)
            PaymentCompletedEvent completedEvent = PaymentCompletedEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(orderId)
                    .customerId(payment.getCustomerId())
                    .amount(amount)
                    .transactionRef(payment.getTransactionRef())
                    .build();

            outboxPublisher.enqueue(
                    Topics.PAYMENT_COMPLETED,
                    "PaymentCompletedEvent",
                    "Payment",
                    orderId,
                    traceId,
                    completedEvent
            );
        }

        // Mark event as processed in idempotency table
        idempotencyHandler.markAsProcessed(eventId, "InventoryReservedEvent", "payment-service-group");
    }

    public List<Payment> getPaymentsForOrder(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}
