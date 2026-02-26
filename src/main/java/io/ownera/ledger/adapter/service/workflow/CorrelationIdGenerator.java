package io.ownera.ledger.adapter.service.workflow;

import java.security.SecureRandom;
import java.util.Base64;

public class CorrelationIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate() {
        byte[] bytes = new byte[64];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
