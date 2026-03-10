package io.ownera.ledger.adapter.service.model;

public class CryptoTransfer implements PaymentMethodInstruction {

    public final String network;
    public final String contractAddress;
    public final String walletAddress;

    public CryptoTransfer(String network, String contractAddress, String walletAddress) {
        this.network = network;
        this.contractAddress = contractAddress;
        this.walletAddress = walletAddress;
    }
}
