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

@RestController
@ConditionalOnBean(AccountMappingStore.class)
public class MappingController {

    private static final Logger logger = LoggerFactory.getLogger(MappingController.class);

    private final AccountMappingStore store;
    private final Optional<MappingValidator> validator;
    private final Optional<MappingProvisionHook> provisionHook;
    private final List<AccountMappingField> fields;

    public MappingController(AccountMappingStore store,
                             Optional<MappingValidator> validator,
                             Optional<MappingProvisionHook> provisionHook,
                             Optional<List<AccountMappingField>> fields) {
        this.store = store;
        this.validator = validator;
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

            if (finId == null || accountMappings == null || accountMappings.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "finId and accountMappings are required"));
            }

            if (!"active".equals(status) && !"inactive".equals(status)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "status must be 'active' or 'inactive'"));
            }

            logger.info("Owner mapping requested: finId={}, fields={}, status={}",
                    finId.substring(0, Math.min(20, finId.length())),
                    accountMappings.keySet(), status);

            if ("inactive".equals(status)) {
                store.delete(finId, null);
                logger.info("Owner mapping disabled: finId={}", finId);
                return ResponseEntity.ok(Map.of(
                        "finId", finId,
                        "status", "inactive",
                        "accountMappings", accountMappings
                ));
            }

            Map<String, String> validatedMappings = validator
                    .map(v -> v.validate(finId, accountMappings))
                    .orElse(accountMappings);

            store.save(finId, validatedMappings);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("finId", finId);
            result.put("status", "active");
            result.put("accountMappings", validatedMappings);

            provisionHook.ifPresent(hook -> {
                try {
                    String ledgerAccountId = validatedMappings.getOrDefault("ledgerAccountId", "");
                    Map<String, String> extra = hook.afterSave(finId, ledgerAccountId, status);
                    if (extra != null) {
                        result.putAll(extra);
                    }
                } catch (Exception e) {
                    logger.warn("Provision hook failed: finId={}, error={}", finId, e.getMessage());
                }
            });

            logger.info("Owner mapping created: finId={}, fields={}", finId, validatedMappings.keySet());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.warn("Owner mapping validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
                        .map(store::getByFinId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else {
                mappings = store.listAll();
            }

            List<Map<String, Object>> response = mappings.stream()
                    .map(m -> {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("finId", m.getFinId());
                        entry.put("status", "active");
                        entry.put("accountMappings", m.getFields());
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
