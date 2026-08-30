package com.system.payment.controller;

import com.system.payment.config.PaymentSimulationConfig;
import com.system.payment.dto.PaymentProcessRequest;
import com.system.payment.dto.PaymentProcessResponse;
import com.system.payment.model.Payment;
import com.system.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentSimulationConfig simulationConfig;

    /**
     * Synchronous payment endpoint (used in Step 2).
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentProcessResponse> processPayment(@RequestBody PaymentProcessRequest request) {
        PaymentProcessResponse response = paymentService.processSyncPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment history for an order.
     */
    @GetMapping("/history/{orderId}")
    public ResponseEntity<List<Payment>> getPaymentHistory(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getPaymentsForOrder(orderId));
    }

    /**
     * Get current failure and latency simulation config.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
                "artificialDelayMs", simulationConfig.getDelayMs(),
                "failureRatePercent", simulationConfig.getFailureRate(),
                "forceFailure", simulationConfig.isForceFailure(),
                "forcePoisonPill", simulationConfig.isForcePoisonPill()
        ));
    }

    /**
     * Update simulation configuration to test degradation, saga rollback, or poison pill.
     */
    @PostMapping("/config")
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> body) {
        if (body.containsKey("artificialDelayMs")) {
            simulationConfig.setDelayMs(Long.parseLong(body.get("artificialDelayMs").toString()));
        }
        if (body.containsKey("failureRatePercent")) {
            simulationConfig.setFailureRate(Integer.parseInt(body.get("failureRatePercent").toString()));
        }
        if (body.containsKey("forceFailure")) {
            simulationConfig.setForceFailure(Boolean.parseBoolean(body.get("forceFailure").toString()));
        }
        if (body.containsKey("forcePoisonPill")) {
            simulationConfig.setForcePoisonPill(Boolean.parseBoolean(body.get("forcePoisonPill").toString()));
        }
        return getConfig();
    }

    /**
     * Reset simulation config to normal defaults.
     */
    @PostMapping("/config/reset")
    public ResponseEntity<String> resetConfig() {
        simulationConfig.reset();
        return ResponseEntity.ok("Payment simulation config reset to normal operations");
    }
}
