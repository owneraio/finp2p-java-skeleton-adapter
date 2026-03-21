package io.ownera.finp2p.signing.eip712.models;

import io.ownera.finp2p.signing.eip712.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Loan implements Message {

    public static final String TYPE_NAME = "Loan";

    private static final Map<String, List<TypeField>> TYPES;

    static {
        Map<String, List<TypeField>> types = new HashMap<>();
        types.putAll(MessageTypes.DOMAIN_TYPE);
        types.putAll(MessageTypes.FINID_TYPE);
        types.putAll(MessageTypes.TERM_TYPE);
        types.put(
                "LoanTerms", List.of(
                        new TypeField("openTime", "string"),
                        new TypeField("closeTime", "string"),
                        new TypeField("borrowedMoneyAmount", "string"),
                        new TypeField("returnedMoneyAmount", "string")
                )
        );
        types.put(TYPE_NAME, List.of(
                new TypeField("nonce", "string"),
                new TypeField("borrower", "FinId"),
                new TypeField("lender", "FinId"),
                new TypeField("asset", "Term"),
                new TypeField("settlement", "Term"),
                new TypeField("loanTerms", "LoanTerms")
        ));
        TYPES = Map.copyOf(types);
    }

    private String nonce;
    private FinId borrower;
    private FinId lender;
    private Term asset;
    private Term settlement;
    private LoanTerms loanTerms;

    public Loan() {
    }

    public Loan(String nonce, FinId borrower, FinId lender, Term asset, Term settlement, LoanTerms loanTerms) {
        this.nonce = nonce;
        this.borrower = borrower;
        this.lender = lender;
        this.asset = asset;
        this.settlement = settlement;
        this.loanTerms = loanTerms;
    }

    public String getNonce() {
        return nonce;
    }

    public Loan setNonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    public FinId getBorrower() {
        return borrower;
    }

    public Loan setBorrower(FinId borrower) {
        this.borrower = borrower;
        return this;
    }

    public FinId getLender() {
        return lender;
    }

    public Loan setLender(FinId lender) {
        this.lender = lender;
        return this;
    }

    public Term getAsset() {
        return asset;
    }

    public Loan setAsset(Term asset) {
        this.asset = asset;
        return this;
    }

    public Term getSettlement() {
        return settlement;
    }

    public Loan setSettlement(Term settlement) {
        this.settlement = settlement;
        return this;
    }

    public LoanTerms getLoanTerms() {
        return loanTerms;
    }

    public Loan setLoanTerms(LoanTerms loanTerms) {
        this.loanTerms = loanTerms;
        return this;
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public Map<String, List<TypeField>> getTypes() {
        return TYPES;
    }
}
