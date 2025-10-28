package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.service.*;
import io.ownera.ledger.adapter.api.model.*;
import io.ownera.ledger.adapter.service.model.AssetCreationStatus;
import io.ownera.ledger.adapter.service.model.OperationStatus;
import io.ownera.ledger.adapter.service.model.PlanApprovalStatus;
import io.ownera.ledger.adapter.service.model.ReceiptOperation;
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
import static io.ownera.ledger.adapter.Mappers.toAPI;
import static io.ownera.ledger.adapter.Mappers.toAPI;

@RestController
@RequestMapping("/api")
public class Controller {

    private final EscrowService escrowService;
    private final TokenService tokenService;
    private final PaymentService paymentService;
    private final PlanApprovalService planApprovalService;
    private final CommonService commonService;

    public Controller(EscrowService escrowService, TokenService tokenService, PaymentService paymentService,
                      PlanApprovalService planApprovalService, CommonService commonService) {
        this.escrowService = escrowService;
        this.tokenService = tokenService;
        this.paymentService = paymentService;
        this.planApprovalService = planApprovalService;
        this.commonService = commonService;
    }

    private final static Logger logger = LoggerFactory.getLogger(Controller.class);


    @PostMapping(value = "/plan/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIApproveExecutionPlanResponse> approvePlan(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody APIApproveExecutionPlanRequest request
    ) {
        String planId = request.getExecutionPlan().getId();
        logger.info("Approve plan: {}", planId);
        try {
            PlanApprovalStatus status = planApprovalService.approvePlan(idempotencyKey, planId);
            return ResponseEntity.status(HttpStatus.OK).body(toAPI(status));
        } catch (TokenServiceException e) {
            logger.error("Cant start the flow", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(createAssetResponseFailed(1, "Cant start the flow: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/assets/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateAssetResponse> createAsset(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateAssetRequest request
    ) {
        logger.info("Create asset: {}", request);
        try {
            Asset asset = fromAPI(request.getAsset());
            AssetCreationStatus status = tokenService.createAsset(idempotencyKey, asset);
            return ResponseEntity.status(HttpStatus.OK).body(toAPI(status));
        } catch (TokenServiceException e) {
            logger.error("Cant start the flow", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(createAssetResponseFailed(1, "Cant start the flow: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/assets/issue", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> issue(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody APIIssueAssetsRequest request
    ) {
        logger.info("Issue token: {}", request);
        try {
           ReceiptOperation result = tokenService.issue(
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
    public ResponseEntity<APIReceiptOperation> transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody APITransferAssetRequest request
    ) {
        logger.info("Transfer token: {}", request);

        String assetId = request.getAsset().getFinp2pAsset().getResourceId();
        try {
            ServiceOperationResult<ServiceTokenResult> result = tokenService.transfer(idempotencyKey,
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
    public ResponseEntity<APIReceiptOperation> redeem(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody APIRedeemAssetsRequest request
    ) {
        logger.info("Redeem token: {}", request);
        try {
            ServiceOperationResult<ServiceTokenResult> result = tokenService.redeem(
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
    public ResponseEntity<APIGetAssetBalanceResponse> getBalance(@RequestBody APIGetAssetBalanceRequest request) {
        try {
            String balance = tokenService.getBalance(
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

    @GetMapping(value = "/assets/receipts/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIGetReceiptResponse> getReceipt(
            @PathVariable("id") String transactionId
    ) {
        try {
            ReceiptOperation receipt = commonService.getReceipt(transactionId);
            return ResponseEntity.status(HttpStatus.OK).body(toAPI(receipt));

        } catch (TokenServiceException e) {
            logger.error("Cant obtain receipt", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping(value = "/operations/status/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public final ResponseEntity<APIOperationStatus> getOperationStatus(@PathVariable("id") String correlationId) {
        try {
            OperationStatus status = commonService.operationStatus(correlationId);
            return ResponseEntity.status(HttpStatus.OK).body(toAPI(status));
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
