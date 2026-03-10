package io.ownera.ledger.adapter.service;

import io.ownera.ledger.adapter.service.model.OperationStatus;
import io.ownera.ledger.adapter.service.model.ReceiptOperation;

public interface CommonService {

    ReceiptOperation getReceipt(String id);

    OperationStatus operationStatus(String cid);
}
