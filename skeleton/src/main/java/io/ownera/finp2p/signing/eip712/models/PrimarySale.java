package io.ownera.finp2p.signing.eip712.models;

import io.ownera.finp2p.signing.eip712.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrimarySale implements Message {

    public static final String TYPE_NAME = "PrimarySale";

    public static final Map<String, List<TypeField>> TYPES;

    static {
        Map<String, List<TypeField>> types = new HashMap<>();
        types.putAll(MessageTypes.DOMAIN_TYPE);
        types.putAll(MessageTypes.FINID_TYPE);
        types.putAll(MessageTypes.TERM_TYPE);
        types.put(TYPE_NAME, List.of(
                new TypeField("nonce", "string"),
                new TypeField("buyer", "FinId"),
                new TypeField("issuer", "FinId"),
                new TypeField("asset", "Term"),
                new TypeField("settlement", "Term")
        ));
        TYPES = Map.copyOf(types);
    }

    private Term asset;
    private FinId buyer;
    private FinId issuer;
    private String nonce;
    private Term settlement;

    public PrimarySale() {
    }

    public PrimarySale(String nonce, Term asset, Term settlement, FinId buyer, FinId issuer) {
        this.nonce = nonce;
        this.asset = asset;
        this.settlement = settlement;
        this.buyer = buyer;
        this.issuer = issuer;
    }

    public Term getAsset() {
        return asset;
    }

    public PrimarySale setAsset(Term asset) {
        this.asset = asset;
        return this;
    }

    public FinId getBuyer() {
        return buyer;
    }

    public PrimarySale setBuyer(FinId buyer) {
        this.buyer = buyer;
        return this;
    }

    public FinId getIssuer() {
        return issuer;
    }

    public PrimarySale setIssuer(FinId issuer) {
        this.issuer = issuer;
        return this;
    }

    public String getNonce() {
        return nonce;
    }

    public PrimarySale setNonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    public Term getSettlement() {
        return settlement;
    }

    public PrimarySale setSettlement(Term settlement) {
        this.settlement = settlement;
        return this;
    }

    public String getTypeName() {
        return TYPE_NAME;
    }

    public Map<String, List<TypeField>> getTypes() {
        return TYPES;
    }
}
