package io.ownera.ledger.adapter.sample;

import io.ownera.ledger.adapter.service.TransactionHook;
import io.ownera.ledger.adapter.service.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class LoggingTransactionHook implements TransactionHook {

    private static final Logger logger = LoggerFactory.getLogger(LoggingTransactionHook.class);

    @Override
    public void preTransaction(@Nullable String idempotencyKey, OperationType type,
                               @Nullable Source source, @Nullable Destination destination,
                               @Nullable Asset asset, @Nullable String quantity,
                               @Nullable Signature signature, @Nullable ExecutionContext executionContext) {
        logger.debug("Pre-transaction: type={}, asset={}, quantity={}", type, asset, quantity);
    }

    @Override
    public void postTransaction(@Nullable String idempotencyKey, OperationType type,
                                @Nullable Source source, @Nullable Destination destination,
                                @Nullable Asset asset, @Nullable String quantity,
                                @Nullable Signature signature, @Nullable ExecutionContext executionContext,
                                OperationStatus result) {
        logger.debug("Post-transaction: type={}, result={}", type, result.getClass().getSimpleName());
    }
}
