package io.ownera.ledger.adapter.service.model;

public class WireTransfer implements PaymentMethodInstruction {
    public final String accountHolderName;
    public final String bankName;
    public final WireDetails wireDetails;
    public final String line1;
    public final String city;
    public final String postalCode;
    public final String country;

    public WireTransfer(String accountHolderName, String bankName, WireDetails wireDetails, String line1, String city, String postalCode, String country) {
        this.accountHolderName = accountHolderName;
        this.bankName = bankName;
        this.wireDetails = wireDetails;
        this.line1 = line1;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }
}
