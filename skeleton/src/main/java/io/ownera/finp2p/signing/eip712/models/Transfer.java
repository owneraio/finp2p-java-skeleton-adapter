package io.ownera.finp2p.signing.eip712.models;

import io.ownera.finp2p.signing.eip712.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Transfer implements Message {

    public static final String TYPE_NAME = "Transfer";

    private static final Map<String, List<TypeField>> TYPES;

    static {
        Map<String, List<TypeField>> types = new HashMap<>();
        types.putAll(MessageTypes.DOMAIN_TYPE);
        types.putAll(MessageTypes.FINID_TYPE);
        types.putAll(MessageTypes.TERM_TYPE);
        types.put(TYPE_NAME, List.of(
                new TypeField("nonce", "string"),
                new TypeField("buyer", "FinId"),
                new TypeField("seller", "FinId"),
                new TypeField("asset", "Term")
        ));
        TYPES = Map.copyOf(types);
    }

    private String nonce;
    private FinId buyer;
    private FinId seller;
    private Term asset;

    public Transfer() {
    }

    public Transfer(String nonce, FinId buyer, FinId seller, Term asset) {
        this.nonce = nonce;
        this.buyer = buyer;
        this.seller = seller;
        this.asset = asset;
    }

    public String getNonce() {
        return nonce;
    }

    public Transfer setNonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    public FinId getBuyer() {
        return buyer;
    }

    public Transfer setBuyer(FinId buyer) {
        this.buyer = buyer;
        return this;
    }

    public FinId getSeller() {
        return seller;
    }

    public Transfer setSeller(FinId seller) {
        this.seller = seller;
        return this;
    }

    public Term getAsset() {
        return asset;
    }

    public Transfer setAsset(Term asset) {
        this.asset = asset;
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
