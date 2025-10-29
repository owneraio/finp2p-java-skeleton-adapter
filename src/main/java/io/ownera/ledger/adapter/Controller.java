package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.*;
import io.ownera.ledger.adapter.service.*;
import io.ownera.ledger.adapter.service.model.*;
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
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIApproveExecutionPlanRequest request
    ) {
        String planId = request.getExecutionPlan().getId();
        logger.info("Approve plan: {}", planId);
        PlanApprovalStatus status = planApprovalService.approvePlan(idempotencyKey, planId);
        return ResponseEntity.status(HttpStatus.OK).body(toAPIResponse(status));
    }

    @PostMapping(value = "/assets/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APICreateAssetResponse> createAsset(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APICreateAssetRequest request
    ) {
        logger.info("Create asset: {}", request);
        try {
            AssetCreationStatus status = tokenService.createAsset(
                    idempotencyKey,
                    fromAPI(request.getAsset()),
                    fromAPI(request.getLedgerAssetBinding()),
                    request.getMetadata(),
                    request.getName(),
                    request.getIssuerId(),
                    fromAPI(request.getDenomination()),
                    fromAPI(request.getAssetIdentifier())
            );
            return ResponseEntity.status(HttpStatus.OK).body(toAPIResponse(status));
        } catch (TokenServiceException e) {
            return ResponseEntity.status(HttpStatus.OK).body(failedAssetOperation(1, e.getMessage()));
        }
    }

    @PostMapping(value = "/assets/issue", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> issue(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIIssueAssetsRequest request
    ) {
        logger.info("Issue assets: {}", request);
        try {
            ReceiptOperation rcptOp = tokenService.issue(
                    idempotencyKey,
                    fromAPI(request.getAsset()),
                    fromAPI(request.getDestination()),
                    request.getQuantity(),
                    fromAPI(request.getExecutionContext())
            );
            return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
        } catch (TokenServiceException e) {
            return ResponseEntity.status(HttpStatus.OK).body(failedTokenOperation(1, e.getMessage()));
        }
    }

    @PostMapping(value = "/assets/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> transfer(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APITransferAssetRequest request
    ) {
        logger.info("Transfer assets: {}", request);

        try {
            ReceiptOperation rcptOp = tokenService.transfer(
                    idempotencyKey,
                    request.getNonce(),
                    fromAPI(request.getSource()),
                    fromAPI(request.getDestination()),
                    fromAPI(request.getAsset()),
                    request.getQuantity(),
                    fromAPI(request.getSignature()),
                    fromAPI(request.getExecutionContext())
            );
            return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
        } catch (TokenServiceException e) {
            return ResponseEntity.status(HttpStatus.OK).body(failedTokenOperation(1, e.getMessage()));
        }
    }

    @PostMapping(value = "/assets/redeem", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> redeem(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIRedeemAssetsRequest request
    ) {
        logger.info("Redeem assets: {}", request);
        try {
            ReceiptOperation rcptOp = tokenService.redeem(
                    idempotencyKey,
                    request.getNonce(),
                    fromAPI(request.getSource()),
                    fromAPI(request.getAsset()),
                    request.getQuantity(),
                    request.getOperationId(),
                    fromAPI(request.getSignature()),
                    fromAPI(request.getExecutionContext())
            );
            return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
        } catch (TokenServiceException e) {
            return ResponseEntity.status(HttpStatus.OK).body(failedTokenOperation(1, e.getMessage()));
        }
    }


    @PostMapping(value = "/assets/hold", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> hold(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIHoldOperationRequest request
    ) {
        logger.info("Hold assets: {}", request);
        try {
            ReceiptOperation rcptOp = escrowService.hold(
                    idempotencyKey,
                    request.getNonce(),
                    fromAPI(request.getSource()),
                    fromAPI(request.getDestination()),
                    fromAPI(request.getAsset()),
                    request.getQuantity(),
                    fromAPI(request.getSignature()),
                    request.getOperationId(),
                    fromAPI(request.getExecutionContext())
            );
            return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
        } catch (TokenServiceException e) {
            return ResponseEntity.status(HttpStatus.OK).body(failedTokenOperation(1, e.getMessage()));
        }
    }

    @PostMapping(value = "/assets/release", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> release(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIReleaseOperationRequest request
    ) {
        logger.info("Release assets: {}", request);
        try {
            ReceiptOperation rcptOp = escrowService.release(
                    idempotencyKey,
                    fromAPI(request.getSource()),
                    fromAPI(request.getDestination()),
                    fromAPI(request.getAsset()),
                    request.getQuantity(),
                    request.getOperationId(),
                    fromAPI(request.getExecutionContext())
            );
            return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
        } catch (TokenServiceException e) {
            return ResponseEntity.status(HttpStatus.OK).body(failedTokenOperation(1, e.getMessage()));
        }
    }

    @PostMapping(value = "/assets/rollback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIReceiptOperation> rollback(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIRollbackOperationRequest request
    ) {
        logger.info("Rollback assets: {}", request);
        try {
            ReceiptOperation rcptOp = escrowService.rollback(
                    idempotencyKey,
                    fromAPI(request.getSource()),
                    fromAPI(request.getAsset()),
                    request.getQuantity(),
                    request.getOperationId(),
                    fromAPI(request.getExecutionContext())
            );
            return ResponseEntity.status(HttpStatus.OK).body(toAPI(rcptOp));
        } catch (TokenServiceException e) {
            return ResponseEntity.status(HttpStatus.OK).body(failedTokenOperation(1, e.getMessage()));
        }
    }

    @PostMapping(value = "/assets/getBalance", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIGetAssetBalanceResponse> getBalance(@RequestBody APIGetAssetBalanceRequest request) {
        String balance = tokenService.getBalance(
                request.getAsset().getAPIFinp2pAsset().getResourceId(),
                request.getOwner().getAccount().getFinId()
        );
        return ResponseEntity.status(HttpStatus.OK).body(new APIGetAssetBalanceResponse()
                .asset(request.getAsset())
                .balance(balance));
    }

    @GetMapping(value = "/assets/receipts/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIGetReceiptResponse> getReceipt(
            @PathVariable("id") String transactionId
    ) {
        ReceiptOperation receipt = commonService.getReceipt(transactionId);
        return ResponseEntity.status(HttpStatus.OK).body(Mappers.toAPIGetReceiptResponse(receipt));
    }

    @GetMapping(value = "/operations/status/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public final ResponseEntity<APIOperationStatus> getOperationStatus(@PathVariable("id") String correlationId) {
        OperationStatus status = commonService.operationStatus(correlationId);
        return ResponseEntity.status(HttpStatus.OK).body(toAPI(status));
    }


    @PostMapping(value = "/payments/depositInstruction", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIDepositInstructionResponse> depositInstruction(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody APIDepositInstructionRequest request
    ) {
        logger.info("Deposit instruction: {}", request);

        DepositOperation rcptOp = paymentService.getDepositInstruction(
                idempotencyKey,
                fromAPI(request.getOwner()),
                fromAPI(request.getDestination()),
                fromAPI(request.getAsset()),
                request.getAmount(),
                request.getDetails(),
                request.getNonce(),
                fromAPI(request.getSignature())
        );
        return ResponseEntity.status(HttpStatus.OK).body(toAPIResponse(rcptOp));
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
