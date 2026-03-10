package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.HealthService;

import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleHealthService implements HealthService {

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public void markReady() {
        initialized.set(true);
    }

    @Override
    public void liveness() {
        // no-op - override in real implementations
    }

    @Override
    public void readiness() {
        if (!initialized.get()) {
            throw new RuntimeException("Service is not ready");
        }
    }
}
