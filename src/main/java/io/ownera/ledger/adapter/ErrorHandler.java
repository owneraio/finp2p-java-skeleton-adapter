package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.APIReceiptOperation;
import io.ownera.ledger.adapter.api.model.APIReceiptOperationErrorInformation;
import io.ownera.ledger.adapter.service.BusinessException;
import io.ownera.ledger.adapter.service.TokenServiceException;
import io.ownera.ledger.adapter.service.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.Map;

@RestControllerAdvice
public class ErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<APIReceiptOperation> handleValidation(ValidationException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorOperation(ex.code, ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<APIReceiptOperation> handleBusiness(BusinessException ex) {
        logger.warn("Business error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(errorOperation(ex.code, ex.getMessage()));
    }

    @ExceptionHandler(TokenServiceException.class)
    public ResponseEntity<APIReceiptOperation> handleTokenService(TokenServiceException ex) {
        logger.warn("Token service error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(errorOperation(1, ex.getMessage()));
    }

    @ExceptionHandler(MappingException.class)
    public ResponseEntity<APIReceiptOperation> handleMapping(MappingException ex) {
        logger.warn("Mapping error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorOperation(1, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        logger.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("error", "Internal Server Error"));
    }

    private static APIReceiptOperation errorOperation(int code, String message) {
        APIReceiptOperation operation = new APIReceiptOperation();
        operation.cid("");
        operation.isCompleted(true);
        operation.error(new APIReceiptOperationErrorInformation()
                .code(code)
                .message(message));
        return operation;
    }
}
