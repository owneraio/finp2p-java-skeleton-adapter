package io.ownera.ledger.adapter.service.workflow;

import io.ownera.ledger.adapter.service.model.OperationStatus;

public interface CallbackClient {

    void sendCallback(String cid, OperationStatus result);
}
