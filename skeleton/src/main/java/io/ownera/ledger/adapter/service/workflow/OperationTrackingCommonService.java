package io.ownera.ledger.adapter.service.workflow;

import io.ownera.ledger.adapter.service.CommonService;
import io.ownera.ledger.adapter.service.model.OperationStatus;
import io.ownera.ledger.adapter.service.model.ReceiptOperation;

public class OperationTrackingCommonService implements CommonService {

    private final CommonService delegate;
    private final OperationStore store;

    public OperationTrackingCommonService(CommonService delegate, OperationStore store) {
        this.delegate = delegate;
        this.store = store;
    }

    @Override
    public ReceiptOperation getReceipt(String id) {
        return delegate.getReceipt(id);
    }

    @Override
    public OperationStatus operationStatus(String cid) {
        OperationRecord record = store.findByCid(cid);
        if (record != null && record.result != null) {
            return record.result;
        }
        return delegate.operationStatus(cid);
    }
}
