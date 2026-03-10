package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.service.mapping.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for owner identity mapping endpoints.
 * Mirrors the Node.js skeleton's /mapping/owners and /mapping/fields routes.
 * <p>
 * Only activated when an {@link AccountMappingStore} bean is present.
 */
@RestController
@ConditionalOnBean(AccountMappingStore.class)
public class MappingController {

    private static final Logger logger = LoggerFactory.getLogger(MappingController.class);

    private final AccountMappingStore store;
    private final Optional<MappingProvisionHook> provisionHook;
    private final List<AccountMappingField> fields;

    public MappingController(AccountMappingStore store,
                             Optional<MappingProvisionHook> provisionHook,
                             Optional<List<AccountMappingField>> fields) {
        this.store = store;
        this.provisionHook = provisionHook;
        this.fields = fields.orElse(Collections.emptyList());
    }

    @PostMapping(value = "/mapping/owners", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createOwnerMapping(@RequestBody Map<String, Object> body) {
        try {
            String finId = (String) body.get("finId");
            @SuppressWarnings("unchecked")
            Map<String, String> accountMappings = (Map<String, String>) body.get("accountMappings");
            String status = (String) body.getOrDefault("status", "active");

            if (finId == null || accountMappings == null || accountMappings.get("ledgerAccountId") == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "finId and accountMappings.ledgerAccountId are required"));
            }

            String ledgerAccountId = accountMappings.get("ledgerAccountId");

            if (!"active".equals(status) && !"inactive".equals(status)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "status must be 'active' or 'inactive'"));
            }

            logger.info("Owner mapping requested: finId={}, account={}, status={}",
                    finId.substring(0, Math.min(20, finId.length())),
                    ledgerAccountId.substring(0, Math.min(20, ledgerAccountId.length())),
                    status);

            if ("inactive".equals(status)) {
                store.delete(finId, ledgerAccountId);
                logger.info("Owner mapping disabled: finId={}", finId);
                return ResponseEntity.ok(Map.of(
                        "finId", finId,
                        "status", "inactive",
                        "accountMappings", Map.of("ledgerAccountId", ledgerAccountId)
                ));
            }

            store.save(finId, ledgerAccountId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("finId", finId);
            result.put("status", "active");
            result.put("accountMappings", Map.of("ledgerAccountId", ledgerAccountId));

            provisionHook.ifPresent(hook -> {
                try {
                    Map<String, String> extra = hook.afterSave(finId, ledgerAccountId, status);
                    if (extra != null) {
                        result.putAll(extra);
                    }
                } catch (Exception e) {
                    logger.warn("Provision hook failed: finId={}, error={}", finId, e.getMessage());
                }
            });

            logger.info("Owner mapping created: finId={}, account={}", finId, ledgerAccountId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Owner mapping failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/mapping/owners")
    public ResponseEntity<?> getOwnerMappings(@RequestParam(required = false) String finIds) {
        try {
            List<AccountMapping> mappings;
            if (finIds != null && !finIds.isBlank()) {
                String[] ids = finIds.split(",");
                mappings = Arrays.stream(ids)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .flatMap(fid -> store.getByFinId(fid).stream())
                        .collect(Collectors.toList());
            } else {
                mappings = store.listAll();
            }

            List<Map<String, Object>> response = mappings.stream()
                    .map(m -> {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("finId", m.getFinId());
                        entry.put("status", "active");
                        entry.put("accountMappings", Map.of("ledgerAccountId", m.getAccount()));
                        return entry;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Owner mapping query failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/mapping/fields")
    public ResponseEntity<List<AccountMappingField>> getMappingFields() {
        return ResponseEntity.ok(fields);
    }
}
