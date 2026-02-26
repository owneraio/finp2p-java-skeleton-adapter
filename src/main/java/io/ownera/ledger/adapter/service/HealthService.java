package io.ownera.ledger.adapter.service;

public interface HealthService {
    void liveness();
    void readiness();
}
