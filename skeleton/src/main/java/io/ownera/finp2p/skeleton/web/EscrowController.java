package io.ownera.finp2p.skeleton.web;

import io.ownera.ledger.adapter.api.model.APIHoldOperationRequest;
import io.ownera.ledger.adapter.api.model.APIReceiptOperation;
import io.ownera.ledger.adapter.api.model.APIReleaseOperationRequest;
import io.ownera.ledger.adapter.api.model.APIRollbackOperationRequest;
import io.ownera.ledger.adapter.service.EscrowService;
import io.ownera.ledger.adapter.service.TransactionHook;
import io.ownera.ledger.adapter.service.model.Asset;
import io.ownera.ledger.adapter.service.model.Destination;
import io.ownera.ledger.adapter.service.model.ExecutionContext;
import io.ownera.ledger.adapter.service.model.OperationType;
import io.ownera.ledger.adapter.service.model.ReceiptOperation;
import io.ownera.ledger.adapter.service.model.Signature;
import io.ownera.ledger.adapter.service.model.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static io.ownera.ledger.adapter.Mappers.assetFromAPI;
import static io.ownera.ledger.adapter.Mappers.destinationFromAPI;
import static io.ownera.ledger.adapter.Mappers.fromAPI;
import static io.ownera.ledger.adapter.Mappers.sourceFromAPI;
import static io.ownera.ledger.adapter.Mappers.toAPI;
import static io.ownera.finp2p.skeleton.web.PlanApprovalController.ensureIdempotencyKey;

/**
 * {@code /api/assets/{hold,release,rollback}} routes — only loaded when an
 * {@link EscrowService} bean exists.
 */
@RestController
public class EscrowController {

    private static final Logger logger = LoggerFactory.getLogger(EscrowController.class);

    private final EscrowService escrowService;
    private final Optional<TransactionHook> transactionHook;

    public EscrowController(EscrowService escrowService, Optional<TransactionHook> transactionHook) {
        this.escrowService = escrowService;
        this.transactionHook = transactionHook;
    }

    @PostMapping(value = "/api/assets/hold", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> hold(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIHoldOperationRequest request) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Hold assets: {}", request);

        Source source = sourceFromAPI(request.getSource());
        Destination destination = destinationFromAPI(request.getDestination());
        Asset asset = assetFromAPI(request.getSource());
        Signature sig = fromAPI(request.getSignature());
        ExecutionContext exCtx = fromAPI(request.getExecutionContext());

        transactionHook.ifPresent(h -> h.preTransaction(
                ik, OperationType.HOLD, source, destination, asset,
                request.getQuantity(), sig, exCtx));
        ReceiptOperation rcptOp = escrowService.hold(ik, request.getNonce(), source, destination, asset,
                request.getQuantity(), sig, request.getOperationId(), exCtx);

        transactionHook.ifPresent(h -> h.postTransaction(
                ik, OperationType.HOLD, source, destination, asset,
                request.getQuantity(), sig, exCtx, rcptOp));

        return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
    }

    @PostMapping(value = "/api/assets/release", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> release(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIReleaseOperationRequest request) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Release assets: {}", request);

        Source source = sourceFromAPI(request.getSource());
        Destination destination = destinationFromAPI(request.getDestination());
        Asset asset = assetFromAPI(request.getSource());
        ExecutionContext exCtx = fromAPI(request.getExecutionContext());

        transactionHook.ifPresent(h -> h.preTransaction(
                ik, OperationType.RELEASE, source, destination, asset,
                request.getQuantity(), null, exCtx));
        ReceiptOperation rcptOp = escrowService.release(ik, source, destination, asset,
                request.getQuantity(), request.getOperationId(), exCtx);

        transactionHook.ifPresent(h -> h.postTransaction(
                ik, OperationType.RELEASE, source, destination, asset,
                request.getQuantity(), null, exCtx, rcptOp));

        return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
    }

    @PostMapping(value = "/api/assets/rollback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> rollback(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIRollbackOperationRequest request) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Rollback assets: {}", request);

        Source source = sourceFromAPI(request.getSource());
        Asset asset = assetFromAPI(request.getSource());
        ExecutionContext exCtx = fromAPI(request.getExecutionContext());

        transactionHook.ifPresent(h -> h.preTransaction(
                ik, OperationType.ROLLBACK, source, null, asset,
                request.getQuantity(), null, exCtx));
        ReceiptOperation rcptOp = escrowService.rollback(ik, source, asset,
                request.getQuantity(), request.getOperationId(), exCtx);

        transactionHook.ifPresent(h -> h.postTransaction(
                ik, OperationType.ROLLBACK, source, null, asset,
                request.getQuantity(), null, exCtx, rcptOp));

        return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
    }
}
