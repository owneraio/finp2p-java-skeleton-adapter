package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.*;
import io.ownera.ledger.adapter.service.LedgerService;
import io.ownera.ledger.adapter.service.TokenServiceException;
import io.ownera.ledger.adapter.service.model.AssetCreationStatus;
import io.ownera.ledger.adapter.service.model.ServiceTokenResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;

import static io.ownera.ledger.adapter.Mappers.*;

@RestController
@RequestMapping("/api")
public class Controller {

    private final LedgerService ledgerService;

    public Controller(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    @PostMapping(value = "/assets/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateAssetResponse> createAsset(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateAssetRequest request
    ) {
        logger.info("Create asset: {}", request);
        try {
            ServiceOperationResult<AssetCreationStatus> result = ledgerService.createAsset(idempotencyKey,
                    request.getAsset().getFinp2pAsset().getResourceId());
            return ResponseEntity.status(HttpStatus.OK).body(createAssetResponse(result));
        } catch (TokenServiceException e) {
            logger.error("Cant start the flow", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(createAssetResponseFailed(1, "Cant start the flow: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/assets/issue", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReceiptOperation> issue(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody IssueAssetsRequest request
    ) {
        logger.info("Issue token: {}", request);
        try {
            ServiceOperationResult<ServiceTokenResult> result = ledgerService.issueAssets(
                    idempotencyKey,
                    request.getNonce(),
                    request.getAsset().getResourceId(),
                    request.getDestination().getFinId(),
                    request.getQuantity(),
                    fromAPI(request.getSignature())
            );
            return ResponseEntity.status(HttpStatus.OK).body(receiptOperation(result));
        } catch (TokenServiceException e) {
            logger.error("Cant start the flow", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(receiptOperationFailed(1, "Cant start the flow: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/assets/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReceiptOperation> transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody TransferAssetRequest request
    ) {
        logger.info("Transfer token: {}", request);

        String assetId = request.getAsset().getFinp2pAsset().getResourceId();
        try {
            ServiceOperationResult<ServiceTokenResult> result = ledgerService.transferAssets(idempotencyKey,
                    request.getNonce(),
                    assetId, request.getSource().getFinId(),
                    request.getDestination().getFinId(),
                    request.getQuantity(),
                    fromAPI(request.getSignature())
            );
            return ResponseEntity.status(HttpStatus.OK).body(receiptOperation(result));
        } catch (TokenServiceException e) {
            logger.error("Cant start the flow", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(receiptOperationFailed(1, "Cant start the flow: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/assets/redeem", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReceiptOperation> redeem(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody RedeemAssetsRequest request
    ) {
        logger.info("Redeem token: {}", request);
        try {
            ServiceOperationResult<ServiceTokenResult> result = ledgerService.redeemAssets(
                    idempotencyKey,
                    request.getNonce(),
                    request.getAsset().getResourceId(),
                    request.getSource().getFinId(),
                    request.getQuantity(),
                    fromAPI(request.getSignature())
            );
            return ResponseEntity.status(HttpStatus.OK).body(receiptOperation(result));
        } catch (TokenServiceException e) {
            logger.error("Cant start the flow", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(receiptOperationFailed(1, "Cant start the flow: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/assets/getBalance", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GetAssetBalanceResponse> getBalance(@RequestBody GetAssetBalanceRequest request) {
        try {
            String balance = ledgerService.getBalance(
                    request.getAsset().getFinp2pAsset().getResourceId(),
                    request.getOwner().getAccount().getFinId()
            );
            GetAssetBalanceResponse response = new GetAssetBalanceResponse()
                    .asset(request.getAsset())
                    .balance(balance);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (TokenServiceException e) {
            logger.error("Cant obtain balance", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping(value = "/assets/receipts/{id}",  consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Receipt> getReceipt(
            @PathVariable("id") String transactionId
    ) {
        try {
            ServiceTokenResult tokenReceipt = ledgerService.getTokenReceipt(transactionId);
            return ResponseEntity.status(HttpStatus.OK).body(receipt(tokenReceipt));

        } catch (TokenServiceException e) {
            logger.error("Cant obtain receipt", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping(value = "/operations/status/{id}",  consumes = MediaType.APPLICATION_JSON_VALUE)
    public final ResponseEntity<OperationStatus> getOperationStatus(@PathVariable("id") String correlationId) {
        try {
            ServiceOperationStatus status = ledgerService.getOperationStatus(correlationId);
            return ResponseEntity.status(HttpStatus.OK).body(operationStatus(status));
        } catch (TokenServiceException e) {
            logger.error("Cant obtain status", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @ExceptionHandler({
            InterruptedException.class,
            ExecutionException.class
    })
    public ModelAndView flowError(HttpServletRequest req, Exception ex) {
        ModelAndView mav = new ModelAndView();
        mav.addObject("exception", ex);
        mav.addObject("url", req.getRequestURL());
        mav.setViewName("error");
        return mav;
    }

}
