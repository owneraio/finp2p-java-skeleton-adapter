package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.*;
import io.ownera.ledger.adapter.service.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The 0.28 spec adds "move" to the operationType enum (alongside the new /assets/move
 * endpoint). Both mapping directions previously fell through to their default branch and
 * threw MappingException, so a receipt for a move operation failed at the boundary rather
 * than being reported.
 */
public class MoveOperationTypeTest {

    private static Receipt moveReceipt() {
        String srcFinId = "024e5be4e07c92f492b3c9680fd249a2b8004209ef8bb167421182a1bc0e94f264";
        String dstFinId = "035b1a2c9d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f9";
        return new Receipt(
                "txn-move-1",
                OperationType.MOVE,
                new Asset("org-a:102:move", AssetType.FINP2P),
                new Source(srcFinId, new FinIdAccount(srcFinId)),
                new Destination(dstFinId, new FinIdAccount(dstFinId)),
                "10",
                new TransactionDetails("txn-move-1", null),
                new TradeDetails(new ExecutionContext("org-a:106:plan", 1)),
                null,
                1777525286354L);
    }

    @Test
    void moveReceiptSerializesAsMoveOperationType() {
        APIReceiptOperation api = Mappers.toAPI(new SuccessReceiptStatus(moveReceipt()));

        assertEquals(APIOperationType.MOVE, api.getResponse().getOperationType());
    }

    @Test
    void inboundMoveOperationTypeMapsToInternalMove() {
        APIReceiptOperation api = Mappers.toAPI(new SuccessReceiptStatus(moveReceipt()));

        ReceiptOperation roundTripped = Mappers.fromAPI(api);

        assertInstanceOf(SuccessReceiptStatus.class, roundTripped);
        assertEquals(OperationType.MOVE,
                ((SuccessReceiptStatus) roundTripped).receipt.operationType);
    }

    @Test
    void moveIsExposedOnTheApiEnum() {
        assertEquals(APIOperationType.MOVE, APIOperationType.fromValue("move"));
    }
}
