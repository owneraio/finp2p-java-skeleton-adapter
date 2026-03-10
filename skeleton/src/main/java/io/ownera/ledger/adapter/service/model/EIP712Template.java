package io.ownera.ledger.adapter.service.model;

import java.util.Map;

public class EIP712Template implements SignatureTemplate {
    public final String primaryType;
    public final long chainId;
    public final String verifyingContract;
    public final Map<String, Object> types;
    public final Map<String, Object> message;
    public final String hash;

    public EIP712Template() {
        this(null, 0, null, null, null, null);
    }

    public EIP712Template(String primaryType, long chainId, String verifyingContract,
                          Map<String, Object> types, Map<String, Object> message, String hash) {
        this.primaryType = primaryType;
        this.chainId = chainId;
        this.verifyingContract = verifyingContract;
        this.types = types;
        this.message = message;
        this.hash = hash;
    }
}
