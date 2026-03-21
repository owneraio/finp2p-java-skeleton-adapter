package io.ownera.finp2p.signing.eip712.models;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MessageTypes {

    public static final Map<String, List<TypeField>> DOMAIN_TYPE = Map.of(
            "EIP712Domain", List.of(
                    new TypeField("name", "string"),
                    new TypeField("version", "string"),
                    new TypeField("chainId", "uint256"),
                    new TypeField("verifyingContract", "address")
            )
    );

    public static final Map<String, List<TypeField>> FINID_TYPE = Map.of(
            "FinId", List.of(new TypeField("idkey", "string"))
    );

    public static final Map<String, List<TypeField>> TERM_TYPE = Map.of(
            "Term", List.of(
                    new TypeField("assetId", "string"),
                    new TypeField("assetType", "string"),
                    new TypeField("amount", "string")
            )
    );

    // Utility to merge maps
    public static Map<String, List<TypeField>> mergeTypes(Map<String, List<TypeField>> a, Map<String, List<TypeField>> b) {
        Map<String, List<TypeField>> merged = new HashMap<>(a);
        merged.putAll(b);
        return merged;
    }
}