package io.ownera.ledger.adapter.service.model;

public class WireTransferUsa implements PaymentMethodInstruction {
      public final String accountNumber;
      public final String routingNumber;
      public final String line1;
      public final String city;
      public final String postalCode;
      public final String country;
      public final String state;

    public WireTransferUsa(String accountNumber, String routingNumber, String line1, String city, String postalCode, String country, String state) {
        this.accountNumber = accountNumber;
        this.routingNumber = routingNumber;
        this.line1 = line1;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.state = state;
    }
}
