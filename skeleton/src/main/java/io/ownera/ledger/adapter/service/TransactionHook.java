package io.ownera.ledger.adapter.service;

import io.ownera.ledger.adapter.service.model.*;
import javax.annotation.Nullable;

public interface TransactionHook {

    void preTransaction(
            @Nullable String idempotencyKey,
            OperationType type,
            @Nullable Source source,
            @Nullable Destination destination,
            @Nullable Asset asset,
            @Nullable String quantity,
            @Nullable Signature signature,
            @Nullable ExecutionContext executionContext
    );

    void postTransaction(
            @Nullable String idempotencyKey,
            OperationType type,
            @Nullable Source source,
            @Nullable Destination destination,
            @Nullable Asset asset,
            @Nullable String quantity,
            @Nullable Signature signature,
            @Nullable ExecutionContext executionContext,
            OperationStatus result
    );
}
