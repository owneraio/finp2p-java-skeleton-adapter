package io.ownera.finp2p.skeleton.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ownera.ledger.adapter.api.model.APIOperationStatus;
import io.ownera.ledger.adapter.service.workflow.OperationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/operations/status/{id}} route — only loaded when an {@link OperationStore}
 * bean exists. Adapters that don't run durable operations (no workflow proxy, no operations
 * table) omit the store and the polling endpoint never appears in the URL space.
 *
 * <p>The route reads {@link OperationStore#findOutputsByCid} directly and returns the stored
 * {@code APIOperationStatus} JSON verbatim (byte-faithful with what was persisted at
 * finalize time). 404 is reserved for genuinely unknown cids.
 */
@RestController
public class OperationStatusController {

    private static final Logger logger = LoggerFactory.getLogger(OperationStatusController.class);

    private final OperationStore operationStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OperationStatusController(OperationStore operationStore) {
        this.operationStore = operationStore;
    }

    @GetMapping(value = "/api/operations/status/{id}")
    public ResponseEntity<APIOperationStatus> getOperationStatus(@PathVariable("id") String correlationId) {
        String stored = operationStore.findOutputsByCid(correlationId);
        if (stored == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        try {
            APIOperationStatus api = objectMapper.readValue(stored, APIOperationStatus.class);
            return ResponseEntity.status(HttpStatus.OK).body(api);
        } catch (Exception e) {
            logger.error("Failed to parse persisted outputs for cid={}: {}", correlationId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
