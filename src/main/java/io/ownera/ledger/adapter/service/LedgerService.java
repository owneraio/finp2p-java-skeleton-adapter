package io.ownera.ledger.adapter.service;

import io.ownera.ledger.adapter.service.model.*;

public interface LedgerService {



    ServiceTokenResult getTokenReceipt(String transactionId) throws TokenServiceException;

    ServiceOperationStatus getOperationStatus(String correlationId) throws TokenServiceException;
}
