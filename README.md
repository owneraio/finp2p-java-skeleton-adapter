# FinP2P Java Skeleton Adapter

A framework for building FinP2P ledger adapters in Java. The skeleton handles REST endpoints, idempotency, operation tracking, database migrations, account mappings, and plan approval orchestration. Adapter implementations focus on the business logic: interacting with the target ledger/blockchain.

## Project Structure

```
finp2p-java-skeleton-adapter/
  skeleton/          # Framework library (published as io.ownera:finp2p-java-skeleton-adapter)
  sample-adapter/    # Reference implementation (not published)
```

**skeleton** provides:
- REST controller with all FinP2P ledger endpoints
- Service interfaces adapters must implement
- Operation executor with idempotency and async support
- DB-backed operation store and account mapping store
- Plan approval service with plugin pattern
- Flyway schema migrations
- EIP712/HashList signature utilities

**sample-adapter** demonstrates a minimal implementation with PostgreSQL-backed balances, hold operations, and transactions.

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+
- PostgreSQL (for tests: Docker for Testcontainers)

### Dependency

Add the skeleton to your adapter's `pom.xml`:

```xml
<dependency>
    <groupId>io.ownera</groupId>
    <artifactId>finp2p-java-skeleton-adapter</artifactId>
    <version>0.27.15</version>
</dependency>
```

### Build and Test

```bash
MAVEN_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED" mvn clean install
```

## Architecture

### REST Endpoints (provided by skeleton)

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/assets/create` | POST | Create a new asset |
| `/api/assets/issue` | POST | Issue (mint) tokens |
| `/api/assets/transfer` | POST | Transfer tokens |
| `/api/assets/redeem` | POST | Redeem (burn) tokens |
| `/api/assets/getBalance` | POST | Get balance |
| `/api/asset/balance` | POST | Get detailed balance info |
| `/api/assets/hold` | POST | Escrow hold |
| `/api/assets/release` | POST | Release escrow |
| `/api/assets/rollback` | POST | Rollback escrow |
| `/api/assets/receipts/{id}` | GET | Get receipt by transaction ID |
| `/api/operations/status/{id}` | GET | Poll operation status |
| `/api/plan/approve` | POST | Approve execution plan |
| `/api/plan/proposal` | POST | Propose cancel/reset/instruction |
| `/api/plan/proposal/status` | POST | Proposal status callback |
| `/api/payments/depositInstruction` | POST | Get deposit instructions |
| `/api/payments/payout` | POST | Execute payout |
| `/mapping/owners` | POST | Create/update account mappings |
| `/mapping/owners` | GET | List account mappings |
| `/mapping/fields` | GET | Get supported mapping fields |
| `/health`, `/health/liveness`, `/health/readiness` | GET | Health probes |

### Service Interfaces

Adapters implement these interfaces as Spring beans:

| Interface | Purpose | Required |
|-----------|---------|----------|
| `TokenService` | Asset lifecycle: create, issue, transfer, redeem, balance | Yes |
| `EscrowService` | Hold, release, rollback operations | Yes |
| `PaymentService` | Deposit instructions and payouts | Yes |
| `CommonService` | Receipt lookup and operation status | Yes |
| `HealthService` | Liveness and readiness checks | Yes |
| `PlanApprovalService` | Execution plan validation | Yes (use `AutoPlanApprovalService` to auto-approve) |

### Optional Extension Points

| Interface | Purpose |
|-----------|---------|
| `TransactionHook` | Pre/post transaction lifecycle callbacks |
| `PlanApprovalPlugin` | Synchronous plan instruction validation |
| `AsyncPlanApprovalPlugin` | Asynchronous plan validation with callback |
| `InboundTransferHook` | Notifications for planned/executed inbound transfers |
| `MappingProvisionHook` | Provision external accounts when mappings are created |
| `CallbackClient` | Send operation results to external systems |

## Implementing an Adapter

### 1. Create a Spring Boot application

```java
@SpringBootApplication
@Configuration
public class MyAdapterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyAdapterApplication.class, args);
    }
}
```

### 2. Implement and wire the required services

```java
@Bean
public TokenService tokenService(DSLContext dsl) {
    return new MyTokenService(dsl);
}

@Bean
public EscrowService escrowService(DSLContext dsl) {
    return new MyEscrowService(dsl);
}

@Bean
public PaymentService paymentService() {
    return new MyPaymentService();
}

@Bean
public CommonService commonService(MyLedger ledger, OperationStore operationStore) {
    return new OperationTrackingCommonService(ledger, operationStore);
}

@Bean
public HealthService healthService() {
    return new SimpleHealthService();
}

@Bean
public PlanApprovalService planApprovalService() {
    return new AutoPlanApprovalService(); // or DefaultPlanApprovalService with plugin
}
```

### 3. Wire the operation executor

```java
@Bean
public OperationExecutor operationExecutor(OperationStore operationStore) {
    // Sync mode (default)
    return new OperationExecutor(operationStore, null);

    // Async mode (returns pending immediately, executes in background)
    // return new OperationExecutor(operationStore, callbackClient, true);
}

@Bean
public OperationStore operationStore(DSLContext dslContext) {
    return new DbOperationStore(dslContext);
}
```

### 4. Configure account mappings

```java
@Bean
public AccountMappingStore accountMappingStore(DSLContext dslContext) {
    return new DbAccountMappingStore(dslContext);
}

@Bean
public List<AccountMappingField> accountMappingFields() {
    return List.of(
        new AccountMappingField("ledgerAccountId", "On-chain account address", "0x123..."),
        new AccountMappingField("custodyAccountId", "Custody vault/wallet ID", "vault-456")
    );
}
```

### 5. Configure the database connection

```java
@Bean
public DSLContext dslContext(DataSource dataSource) {
    return DSL.using(dataSource, SQLDialect.POSTGRES);
}
```

### 6. Add adapter-specific migrations (optional)

Place SQL files in `src/main/resources/db/migration/adapter/`:

```sql
-- V2001__create_my_tables.sql
CREATE TABLE IF NOT EXISTS my_balances (...);
```

Configure in `application.properties`:

```properties
spring.flyway.locations=classpath:db/migration/skeleton,classpath:db/migration/adapter
```

## Database

### Schema Layout

The skeleton manages the `ledger_adapter` schema:

- **`operations`** -- Idempotency tracking (cid, method, status, inputs_hash)
- **`assets`** -- Asset registry (type, id, token_standard, decimals)
- **`account_mappings`** -- Key-value mappings per finId (fin_id, field_name, value)

Adapters create their own tables (typically in `public` schema) for ledger-specific data like balances, transactions, and hold operations.

### Two-Connection Pattern

For production deployments, use separate DB users for migration and runtime:

```properties
# Admin connection (DDL, schema creation, grants)
spring.flyway.url=${MIGRATION_CONNECTION_STRING:${DB_CONNECTION_STRING}}
spring.flyway.user=${MIGRATION_USERNAME:${DB_USERNAME}}
spring.flyway.password=${MIGRATION_PASSWORD:${DB_PASSWORD}}

# Runtime connection (least-privilege, DML only)
spring.datasource.url=${DB_CONNECTION_STRING}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# Grant runtime user access (resolved by Flyway placeholder in R__grant_ledger_user.sql)
spring.flyway.placeholders.ledger_user=${LEDGER_USER:${DB_USERNAME:}}
```

## Workflow and Idempotency

All operations flow through `OperationExecutor`:

1. Compute inputs hash: `computeInputsHash("method", idempotencyKey, requestData)`
2. Check for existing operation (duplicate detection)
3. If cached and completed: return stored result
4. If in progress: return pending status with correlation ID
5. Execute operation, store result, optionally send callback

### Sync vs Async Mode

- **Sync** (default): blocks the request thread, returns result directly
- **Async**: submits to thread pool, returns pending immediately, updates DB on completion

```java
// Sync
new OperationExecutor(store, null)

// Async with callbacks
new OperationExecutor(store, callbackClient, true)
```

## Plan Approval

The skeleton supports three approaches:

1. **Auto-approve**: Use `AutoPlanApprovalService` (no validation)
2. **Sync plugin**: Implement `PlanApprovalPlugin` for immediate validation
3. **Async plugin**: Implement `AsyncPlanApprovalPlugin` for deferred validation with callback

For SDK-integrated approval, use `DefaultPlanApprovalService`:

```java
@Bean
public PlanApprovalService planApprovalService(
        @Value("${ORG_ID}") String orgId,
        OperationalSDK sdk,
        PlanApprovalPlugin plugin) {
    return new DefaultPlanApprovalService(orgId, sdk, plugin, null, null, null);
}
```

This fetches the execution plan from the FinP2P API, filters instructions by org, and delegates validation to the plugin. Plugin validation methods receive the `organizations` list responsible for each instruction.

## Configuration

### Environment Variables

| Variable | Purpose | Default |
|----------|---------|---------|
| `DB_CONNECTION_STRING` | Runtime database JDBC URL | required |
| `DB_USERNAME` | Runtime database user | required |
| `DB_PASSWORD` | Runtime database password | required |
| `DB_DRIVER` | JDBC driver class | `org.postgresql.Driver` |
| `MIGRATION_CONNECTION_STRING` | Migration database URL | falls back to `DB_CONNECTION_STRING` |
| `MIGRATION_USERNAME` | Migration database user | falls back to `DB_USERNAME` |
| `MIGRATION_PASSWORD` | Migration database password | falls back to `DB_PASSWORD` |
| `LEDGER_USER` | Runtime user to grant schema access | falls back to `DB_USERNAME` |
| `LOG_LEVEL` | Root log level | `INFO` |
| `ORG_ID` | Organization ID for plan approval | adapter-specific |
| `FINP2P_ADDRESS` | FinP2P API URL | adapter-specific |
| `OSS_URL` | OSS API URL | adapter-specific |

## Versioning

Version format: `major.minor.patch` where `major.minor` tracks the FinP2P protocol version and `patch` is the skeleton release.

Published to GitHub Packages as `io.ownera:finp2p-java-skeleton-adapter`.

## License

Apache-2.0
