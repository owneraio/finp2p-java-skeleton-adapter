package io.ownera.finp2p.signing.eip712;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.ownera.finp2p.signing.eip712.models.*;
import org.web3j.crypto.StructuredDataEncoder;

import java.io.IOException;
import java.util.Map;


public class EIP712 {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static byte[] hashPrimarySaleMessage(
            String nonce,  String buyer, String issuer, Term asset, Term settlement) throws IOException {
        return hashMessage(new PrimarySale(nonce, asset, settlement, new FinId(buyer), new FinId(issuer)));
    }

    public static byte[] hashBuyingMessage(
            String nonce, String buyer, String seller, Term asset, Term settlement) throws IOException {
        return hashMessage(new Buying(nonce, new FinId(buyer), new FinId(seller), asset, settlement));
    }

    public static byte[] hashSellingMessage(
            String nonce, String buyer, String seller, Term asset, Term settlement) throws IOException {
        return hashMessage(new Selling(nonce, new FinId(buyer), new FinId(seller), asset, settlement));
    }

    public static byte[] hashRedemptionMessage(
            String nonce, String seller, String issuer, Term asset, Term settlement) throws IOException {
        return hashMessage(new Redemption(nonce, new FinId(seller), new FinId(issuer), asset, settlement));
    }

    public static byte[] hashTransferMessage(
            String nonce, String buyer, String seller, Term asset) throws IOException {
        return hashMessage(new Transfer(nonce, new FinId(buyer), new FinId(seller), asset));
    }

    public static byte[] hashPrivateOfferMessage(
            String nonce, String buyer, String seller, Term asset, Term settlement) throws IOException {
        return hashMessage(new PrivateOffer(nonce, new FinId(buyer), new FinId(seller), asset, settlement));
    }

    public static byte[] hashLoanMessage(
            String nonce,
            String borrower,
            String lender,
            Term asset,
            Term settlement,
            LoanTerms loanTerms) throws IOException {
        return hashMessage(new Loan(nonce, new FinId(borrower), new FinId(lender), asset, settlement, loanTerms));
    }

    public static byte[] hashReceiptMessage(
            String id,
            String operationType,
            Source source,
            Destination destination,
            Asset asset,
            TradeDetails tradeDetails,
            TransactionDetails transactionDetails,
            String quantity
    ) throws IOException {
        return hashMessage(new Receipt(id, operationType, source, destination, asset, tradeDetails, transactionDetails, quantity));
    }

    public static byte[] hashMessage(Message message) throws IOException {
        String messageToSign = mapper.writeValueAsString(
                new EIP712Message(
                        Domain.defaultDomain(),
                        message,
                        message.getTypeName(),
                        message.getTypes())
        );
        StructuredDataEncoder encoder = new StructuredDataEncoder(messageToSign);
        return encoder.hashStructuredData();
    }

    public static Message parseMessage(Object message, String primaryType) throws IOException {
        String messageJson = mapper.writeValueAsString(message);
        return parseMessageRaw(messageJson, primaryType);
    }

    public static Message parseMessageRaw(String messageJson, String primaryType) throws IOException {
        return mapper.readValue(messageJson, getMessageClass(primaryType));
    }

    private static Class<? extends Message> getMessageClass(String primaryType) {
        switch (primaryType) {
            case Transfer.TYPE_NAME:
                return Transfer.class;
            case PrivateOffer.TYPE_NAME:
                return PrivateOffer.class;
            case Loan.TYPE_NAME:
                return Loan.class;
            case Receipt.TYPE_NAME:
                return Receipt.class;
            case PrimarySale.TYPE_NAME:
                return PrimarySale.class;
            case Buying.TYPE_NAME:
                return Buying.class;
            case Selling.TYPE_NAME:
                return Selling.class;
            case Redemption.TYPE_NAME:
                return Redemption.class;
            default:
                throw new IllegalArgumentException("Unknown primary type: " + primaryType);
        }
    }
}
