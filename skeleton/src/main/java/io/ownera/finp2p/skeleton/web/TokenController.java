package io.ownera.finp2p.skeleton.web;

import io.ownera.ledger.adapter.api.model.APIAssetBalanceInfoRequest;
import io.ownera.ledger.adapter.api.model.APIAssetBalanceInfoResponse;
import io.ownera.ledger.adapter.api.model.APICreateAssetRequest;
import io.ownera.ledger.adapter.api.model.APICreateAssetResponse;
import io.ownera.ledger.adapter.api.model.APIGetAssetBalanceRequest;
import io.ownera.ledger.adapter.api.model.APIGetAssetBalanceResponse;
import io.ownera.ledger.adapter.api.model.APIIssueAssetsRequest;
import io.ownera.ledger.adapter.api.model.APIReceiptOperation;
import io.ownera.ledger.adapter.api.model.APIRedeemAssetsRequest;
import io.ownera.ledger.adapter.api.model.APITransferAssetRequest;
import io.ownera.ledger.adapter.service.TokenService;
import io.ownera.ledger.adapter.service.TransactionHook;
import io.ownera.ledger.adapter.service.model.Asset;
import io.ownera.ledger.adapter.service.model.AssetCreationStatus;
import io.ownera.ledger.adapter.service.model.Balance;
import io.ownera.ledger.adapter.service.model.Destination;
import io.ownera.ledger.adapter.service.model.ExecutionContext;
import io.ownera.ledger.adapter.service.model.FinIdAccount;
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
import static io.ownera.ledger.adapter.Mappers.balanceToAPI;
import static io.ownera.ledger.adapter.Mappers.destinationFromAPI;
import static io.ownera.ledger.adapter.Mappers.finIdAccountFromAPI;
import static io.ownera.ledger.adapter.Mappers.fromAPI;
import static io.ownera.ledger.adapter.Mappers.sourceFromAPI;
import static io.ownera.ledger.adapter.Mappers.toAPI;
import static io.ownera.ledger.adapter.Mappers.toAPIResponse;
import static io.ownera.finp2p.skeleton.web.PlanApprovalController.ensureIdempotencyKey;

/**
 * {@code /api/assets/*} routes for token creation + lifecycle operations — only loaded when
 * a {@link TokenService} bean exists. Adapters that don't issue, transfer, redeem, or hold
 * tokens omit the bean and the routes never appear in the URL space.
 */
@RestController
public class TokenController {

    private static final Logger logger = LoggerFactory.getLogger(TokenController.class);

    private final TokenService tokenService;
    private final Optional<TransactionHook> transactionHook;

    public TokenController(TokenService tokenService, Optional<TransactionHook> transactionHook) {
        this.tokenService = tokenService;
        this.transactionHook = transactionHook;
    }

    @PostMapping(value = "/api/assets/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APICreateAssetResponse> createAsset(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APICreateAssetRequest request) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Create asset: {}", request);
        AssetCreationStatus status = tokenService.createAsset(
                ik,
                fromAPI(request.getAsset()),
                fromAPI(request.getLedgerAssetBinding()),
                request.getMetadata(),
                request.getName(),
                request.getIssuerId(),
                fromAPI(request.getDenomination())
        );
        return ResponseEntity.status(HttpStatus.OK).body(toAPIResponse(status));
    }

    @PostMapping(value = "/api/assets/issue", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> issue(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIIssueAssetsRequest request) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Issue assets: {}", request);

        FinIdAccount destination = finIdAccountFromAPI(request.getDestination());
        Asset asset = assetFromAPI(request.getDestination());
        ExecutionContext exCtx = fromAPI(request.getExecutionContext());

        transactionHook.ifPresent(h -> h.preTransaction(
                ik, OperationType.ISSUE, null,
                destination.destination(), asset, request.getQuantity(), null, exCtx));
        ReceiptOperation rcptOp = tokenService.issue(ik, asset, destination, request.getQuantity(), exCtx);

        transactionHook.ifPresent(h -> h.postTransaction(
                ik, OperationType.ISSUE, null,
                destination.destination(), asset, request.getQuantity(), null, exCtx, rcptOp));

        return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
    }

    @PostMapping(value = "/api/assets/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> transfer(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APITransferAssetRequest request) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Transfer assets: {}", request);

        Source source = sourceFromAPI(request.getSource());
        Destination destination = destinationFromAPI(request.getDestination());
        Asset asset = assetFromAPI(request.getSource());
        Signature sig = fromAPI(request.getSignature());
        ExecutionContext exCtx = fromAPI(request.getExecutionContext());

        transactionHook.ifPresent(h -> h.preTransaction(
                ik, OperationType.TRANSFER, source, destination, asset,
                request.getQuantity(), sig, exCtx));
        ReceiptOperation rcptOp = tokenService.transfer(ik, request.getNonce(), source, destination, asset,
                request.getQuantity(), sig, exCtx);

        transactionHook.ifPresent(h -> h.postTransaction(
                ik, OperationType.TRANSFER, source, destination, asset,
                request.getQuantity(), sig, exCtx, rcptOp));

        return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
    }

    @PostMapping(value = "/api/assets/redeem", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> redeem(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIRedeemAssetsRequest request) {
        String ik = ensureIdempotencyKey(idempotencyKey);
        logger.info("Redeem assets: {}", request);

        FinIdAccount source = finIdAccountFromAPI(request.getSource());
        Asset asset = assetFromAPI(request.getSource());
        Signature sig = fromAPI(request.getSignature());
        ExecutionContext exCtx = fromAPI(request.getExecutionContext());

        transactionHook.ifPresent(h -> h.preTransaction(
                ik, OperationType.REDEEM, source.source(), null, asset,
                request.getQuantity(), sig, exCtx));
        ReceiptOperation rcptOp = tokenService.redeem(ik, request.getNonce(), source, asset,
                request.getQuantity(), request.getOperationId(), sig, exCtx);

        transactionHook.ifPresent(h -> h.postTransaction(
                ik, OperationType.REDEEM, source.source(), null, asset,
                request.getQuantity(), sig, exCtx, rcptOp));

        return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
    }

    @PostMapping(value = "/api/assets/getBalance", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIGetAssetBalanceResponse> getBalance(@RequestBody APIGetAssetBalanceRequest request) {
        Asset asset = assetFromAPI(request.getOwner());
        String balance = tokenService.getBalance(asset, request.getOwner().getFinId());
        return ResponseEntity.status(HttpStatus.OK).body(new APIGetAssetBalanceResponse()
                .asset(request.getOwner().getAsset())
                .balance(balance));
    }

    @PostMapping(value = "/api/asset/balance", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIAssetBalanceInfoResponse> assetBalance(@RequestBody APIAssetBalanceInfoRequest request) {
        Asset asset = fromAPI(request.getAsset());
        String finId = request.getAccount().getFinId();
        Balance balance = tokenService.balance(asset, finId);
        return ResponseEntity.status(HttpStatus.OK).body(balanceToAPI(request.getAsset(), request.getAccount(), balance));
    }
}
