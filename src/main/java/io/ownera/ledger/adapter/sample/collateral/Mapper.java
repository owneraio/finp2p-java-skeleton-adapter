package io.ownera.ledger.adapter.sample.collateral;

import io.ownera.finp2p.finapi.model.*;
import io.ownera.ledger.adapter.MappingException;
import io.ownera.ledger.adapter.service.model.*;
import io.ownera.ledger.adapter.service.model.Asset;
import io.ownera.ledger.adapter.service.model.Destination;
import io.ownera.ledger.adapter.service.model.OperationType;
import io.ownera.ledger.adapter.service.model.Receipt;
import io.ownera.ledger.adapter.service.model.Source;
import io.ownera.ledger.adapter.service.model.TransactionDetails;
import io.reactivex.annotations.Nullable;

public class Mapper {

    public static Transaction toAPI(Receipt receipt) {
        return new Transaction()
                .id(receipt.id)
                .operationType(toAPI(receipt.operationType))
                .source(toAPI(receipt.source))
                .destination(toAPI(receipt.destination))
                .asset(toAPI(receipt.asset))
                .quantity(receipt.quantity)
                .transactionDetails(toAPI(receipt.transactionDetails))
                .timestamp(receipt.timestamp);
    }

    private static Transaction.OperationTypeEnum toAPI(OperationType operationType) {
        switch (operationType) {
            case ISSUE:
                return Transaction.OperationTypeEnum.ISSUE;
            case TRANSFER:
                return Transaction.OperationTypeEnum.TRANSFER;
            case REDEEM:
                return Transaction.OperationTypeEnum.REDEEM;
            case HOLD:
                return Transaction.OperationTypeEnum.HOLD;
            case RELEASE:
                return Transaction.OperationTypeEnum.RELEASE;
            default:
                throw new MappingException("Unsupported operation type: " + operationType);
        }
    }

    private static AccountInformation toAPI(Source source) {
        return new AccountInformation()
                .finId(source.finId)
                .account(new AccountInformationAccount(
                        new io.ownera.finp2p.finapi.model.FinIdAccount()
                                .finId(source.finId))
                );
    }

    private static AccountInformation toAPI(Destination destination) {
        return new AccountInformation()
                .finId(destination.finId)
                .account(new AccountInformationAccount(
                        new io.ownera.finp2p.finapi.model.FinIdAccount().finId(destination.finId))
                );
    }

    private static io.ownera.finp2p.finapi.model.Asset toAPI(Asset asset) {
        return new io.ownera.finp2p.finapi.model.Asset(new io.ownera.finp2p.finapi.model.Finp2pAsset()
                .type(io.ownera.finp2p.finapi.model.Finp2pAsset.TypeEnum.FINP2P)
                .resourceId(asset.assetId));
    }

    private static io.ownera.finp2p.finapi.model.TransactionDetails toAPI(@Nullable TransactionDetails details) {
        if (details == null) {
            return null;
        }
        return new io.ownera.finp2p.finapi.model.TransactionDetails()
                .transactionId(details.transactionId)
                .operationId(details.operationId);

    }

    public static io.ownera.finp2p.finapi.model.OperationStatus successfulDeposit(String cid, String operationId) {
        return new io.ownera.finp2p.finapi.model.OperationStatus(
                new OperationStatusDeposit()
                        .type(OperationStatusDeposit.TypeEnum.DEPOSIT)
                        .operation(
                                new io.ownera.finp2p.finapi.model.DepositOperation()
                                        .type(io.ownera.finp2p.finapi.model.DepositOperation.TypeEnum.DEPOSIT)
                                        .isCompleted(true)
                                        .cid(cid)
                                        .response(new io.ownera.finp2p.finapi.model.DepositInstruction()
                                                        .depositInstruction(
                                                                new DepositInstructionDepositInstruction()
//                                        .account()
//                                        .details()
//                                        .description()
//                                        .paymentOptions()
                                                        )
                                                        .operationId(operationId)
                                        )));
    }
}
