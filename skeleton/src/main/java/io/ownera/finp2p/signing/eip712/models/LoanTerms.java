package io.ownera.finp2p.signing.eip712.models;


import io.ownera.finp2p.signing.eip712.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoanTerms implements Message {

    public static final String TYPE_NAME = "LoanTerms";

    private static final Map<String, List<TypeField>> TYPES;

    static {
        Map<String, List<TypeField>> types = new HashMap<>();
        types.put(TYPE_NAME, List.of(
                new TypeField("openTime", "string"),
                new TypeField("closeTime", "string"),
                new TypeField("borrowedMoneyAmount", "string"),
                new TypeField("returnedMoneyAmount", "string")
        ));
        TYPES = Map.copyOf(types);
    }

    private String openTime;
    private String closeTime;
    private String borrowedMoneyAmount;
    private String returnedMoneyAmount;

    public LoanTerms() {
    }

    public LoanTerms(String openTime, String closeTime, String borrowedMoneyAmount, String returnedMoneyAmount) {
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.borrowedMoneyAmount = borrowedMoneyAmount;
        this.returnedMoneyAmount = returnedMoneyAmount;
    }

    public String getOpenTime() {
        return openTime;
    }

    public LoanTerms setOpenTime(String openTime) {
        this.openTime = openTime;
        return this;
    }

    public String getCloseTime() {
        return closeTime;
    }

    public LoanTerms setCloseTime(String closeTime) {
        this.closeTime = closeTime;
        return this;
    }

    public String getBorrowedMoneyAmount() {
        return borrowedMoneyAmount;
    }

    public LoanTerms setBorrowedMoneyAmount(String borrowedMoneyAmount) {
        this.borrowedMoneyAmount = borrowedMoneyAmount;
        return this;
    }

    public String getReturnedMoneyAmount() {
        return returnedMoneyAmount;
    }

    public LoanTerms setReturnedMoneyAmount(String returnedMoneyAmount) {
        this.returnedMoneyAmount = returnedMoneyAmount;
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
