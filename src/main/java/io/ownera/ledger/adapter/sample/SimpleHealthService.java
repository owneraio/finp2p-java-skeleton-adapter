package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.HealthService;

public class SimpleHealthService implements HealthService {

    @Override
    public void liveness() {
        // no-op - override in real implementations
    }

    @Override
    public void readiness() {
        // no-op - override in real implementations
    }
}
