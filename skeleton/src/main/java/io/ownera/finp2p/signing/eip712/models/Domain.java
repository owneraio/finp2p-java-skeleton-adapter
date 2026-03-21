package io.ownera.finp2p.signing.eip712.models;

public class Domain {
    private final String chainId;
    private final String name;
    private final String verifyingContract;
    private final String version;

    public Domain(String chainId, String name, String verifyingContract, String version) {
        this.chainId = chainId;
        this.name = name;
        this.verifyingContract = verifyingContract;
        this.version = version;
    }

    public String getChainId() {
        return chainId;
    }

    public String getName() {
        return name;
    }

    public String getVerifyingContract() {
        return verifyingContract;
    }

    public String getVersion() {
        return version;
    }

    public static final String CHAIN_ID = "1";
    public static final String DOMAIN_NAME = "FinP2P";
    public static final String VERIFYING_CONTRACT = "0x0000000000000000000000000000000000000000";
    public static final String DOMAIN_VERSION = "1";

    private static Domain instance = null;
    public static Domain defaultDomain() {
        if (instance == null) {
            instance = new Domain(CHAIN_ID, DOMAIN_NAME, VERIFYING_CONTRACT, DOMAIN_VERSION);
        }
        return instance;
    }
}
