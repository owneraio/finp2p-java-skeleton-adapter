package io.ownera.ledger.adapter.service.proof;

import io.ownera.ledger.adapter.service.model.*;

public class Mapper {

    public static io.ownera.finp2p.signing.eip712.models.Receipt toEIP712(Receipt receipt) {
        return new io.ownera.finp2p.signing.eip712.models.Receipt(
                receipt.id,
                toEIP712(receipt.operationType),
                toEIP712(receipt.source),
                toEIP712(receipt.destination),
                toEIP712(receipt.asset),
                toEIP712(receipt.tradeDetails),
                toEIP712(receipt.transactionDetails),
                receipt.quantity
        );
    }

    private static String toEIP712(OperationType type) {
        return type.name().toLowerCase();
    }

    private static io.ownera.finp2p.signing.eip712.models.Source toEIP712(Source source) {
        return new io.ownera.finp2p.signing.eip712.models.Source("finId", source.finId);
    }

    private static io.ownera.finp2p.signing.eip712.models.Destination toEIP712(Destination destination) {
        return new io.ownera.finp2p.signing.eip712.models.Destination("finId", destination.finId);
    }

    private static io.ownera.finp2p.signing.eip712.models.Asset toEIP712(Asset asset) {
        return new io.ownera.finp2p.signing.eip712.models.Asset(asset.assetId, asset.assetType.name().toLowerCase());
    }

    private static io.ownera.finp2p.signing.eip712.models.TradeDetails toEIP712(TradeDetails tradeDetails) {
        return new io.ownera.finp2p.signing.eip712.models.TradeDetails(toEIP712(tradeDetails.executionContext));
    }

    private static io.ownera.finp2p.signing.eip712.models.ExecutionContext toEIP712(ExecutionContext exCtx) {
        return new io.ownera.finp2p.signing.eip712.models.ExecutionContext(exCtx.planId, String.format("%d", exCtx.sequence));
    }

    private static io.ownera.finp2p.signing.eip712.models.TransactionDetails toEIP712(TransactionDetails txDetails) {
        return new io.ownera.finp2p.signing.eip712.models.TransactionDetails(txDetails.operationId, txDetails.transactionId);
    }
}
