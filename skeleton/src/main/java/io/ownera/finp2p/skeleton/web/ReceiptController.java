package io.ownera.finp2p.skeleton.web;

import io.ownera.ledger.adapter.Mappers;
import io.ownera.ledger.adapter.api.model.APIGetReceiptResponse;
import io.ownera.ledger.adapter.service.CommonService;
import io.ownera.ledger.adapter.service.model.ReceiptOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/assets/receipts/{id}} route — only loaded when a {@link CommonService}
 * bean exists. Adapters that don't expose transaction-id lookups omit the bean.
 */
@RestController
public class ReceiptController {

    private final CommonService commonService;

    public ReceiptController(CommonService commonService) {
        this.commonService = commonService;
    }

    @GetMapping(value = "/api/assets/receipts/{id}")
    public ResponseEntity<APIGetReceiptResponse> getReceipt(@PathVariable("id") String transactionId) {
        ReceiptOperation receipt = commonService.getReceipt(transactionId);
        return ResponseEntity.status(HttpStatus.OK).body(Mappers.toAPIGetReceiptResponse(receipt));
    }
}
