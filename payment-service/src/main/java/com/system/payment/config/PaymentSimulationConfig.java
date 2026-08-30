package com.system.payment.config;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Component
public class PaymentSimulationConfig {

    private final AtomicLong artificialDelayMs = new AtomicLong(0);
    private final AtomicInteger failureRatePercent = new AtomicInteger(0);
    private final AtomicBoolean forceFailure = new AtomicBoolean(false);
    private final AtomicBoolean forcePoisonPill = new AtomicBoolean(false);

    public long getDelayMs() {
        return artificialDelayMs.get();
    }

    public void setDelayMs(long delay) {
        this.artificialDelayMs.set(delay);
    }

    public int getFailureRate() {
        return failureRatePercent.get();
    }

    public void setFailureRate(int rate) {
        this.failureRatePercent.set(Math.max(0, Math.min(100, rate)));
    }

    public boolean isForceFailure() {
        return forceFailure.get();
    }

    public void setForceFailure(boolean fail) {
        this.forceFailure.set(fail);
    }

    public boolean isForcePoisonPill() {
        return forcePoisonPill.get();
    }

    public void setForcePoisonPill(boolean poison) {
        this.forcePoisonPill.set(poison);
    }

    public void reset() {
        artificialDelayMs.set(0);
        failureRatePercent.set(0);
        forceFailure.set(false);
        forcePoisonPill.set(false);
    }
}
