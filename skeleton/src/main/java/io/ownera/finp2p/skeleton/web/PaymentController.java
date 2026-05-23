package io.ownera.finp2p.skeleton.web;

import io.ownera.ledger.adapter.api.model.APIDepositInstructionRequest;
import io.ownera.ledger.adapter.api.model.APIDepositInstructionResponse;
import io.ownera.ledger.adapter.api.model.APIPayoutRequest;
import io.ownera.ledger.adapter.api.model.APIPayoutResponse;
import io.ownera.ledger.adapter.service.PaymentService;
import io.ownera.ledger.adapter.service.TransactionHook;
import io.ownera.ledger.adapter.service.model.Asset;
import io.ownera.ledger.adapter.service.model.DepositOperation;
import io.ownera.ledger.adapter.service.model.Destination;
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

import static io.ownera.ledger.adapter.Mappers.destinationFromAPI;
import static io.ownera.ledger.adapter.Mappers.fromAPI;
import static io.ownera.ledger.adapter.Mappers.sourceFromAPI;
import static io.ownera.ledger.adapter.Mappers.toAPIPayoutResponse;
import static io.ownera.ledger.adapter.Mappers.toAPIResponse;
import static io.ownera.finp2p.skeleton.web.PlanApprovalController.ensureIdempotencyKey;

/**
 * {@code /api/payments/*} routes — only loaded when a {@link PaymentService} bean exists.
 */
@RestController
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final Optional<TransactionHook> transactionHook;

    public PaymentController(PaymentService paymentService, Optional<TransactionHook> transactionHook) {
        this.paymentService = paymentService;
        this.transactionHook = transactionHook;
    }

    @PostMapping(value = "/api/payments/depositInstruction", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIDepositInstructionResponse> depositInstruction(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIDepositInstructionRequest request) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Deposit instruction: {}", request);

        DepositOperation rcptOp = paymentService.getDepositInstruction(
                ik,
                sourceFromAPI(request.getOwner()),
                destinationFromAPI(request.getDestination()),
                fromAPI(request.getAsset()),
                request.getAmount(),
                request.getDetails(),
                request.getNonce(),
                fromAPI(request.getSignature())
        );
        return ResponseEntity.status(HttpStatus.OK).body(toAPIResponse(rcptOp));
    }

    @PostMapping(value = "/api/payments/payout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIPayoutResponse> payout(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIPayoutRequest request) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Payout: {}", request);

        Source source = sourceFromAPI(request.getSource());
        Destination destination = destinationFromAPI(request.getDestination());
        Asset asset = fromAPI(request.getAsset());
        Signature sig = fromAPI(request.getSignature());

        transactionHook.ifPresent(h -> h.preTransaction(
                ik, OperationType.TRANSFER, source, destination, asset,
                request.getQuantity(), sig, null));
        String description = null;
        if (request.getPayoutInstruction() != null) {
            description = request.getPayoutInstruction().getDescription();
        }
        String desc = description;
        ReceiptOperation rcptOp = paymentService.payout(ik, source, destination, asset,
                request.getQuantity(), desc, request.getNonce(), sig);

        transactionHook.ifPresent(h -> h.postTransaction(
                ik, OperationType.TRANSFER, source, destination, asset,
                request.getQuantity(), sig, null, rcptOp));

        return ResponseEntity.status(HttpStatus.OK).body(toAPIPayoutResponse(rcptOp));
    }
}
