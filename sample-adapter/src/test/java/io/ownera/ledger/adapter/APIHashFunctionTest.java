package io.ownera.ledger.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ownera.ledger.adapter.api.model.APIHashFunction;
import io.ownera.ledger.adapter.api.model.APISignature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APIHashFunction.fromValue must accept empty string / null and map them to UNSPECIFIED.
 * The router historically sends hashFunc="" for unsigned operations; throwing on that
 * breaks request deserialization across all signed endpoints (issue/transfer/redeem/hold).
 *
 * Regression: this was fixed in 0.27.9 but lost during the 0.28 OpenAPI regen.
 */
public class APIHashFunctionTest {

    @Test
    void fromValueEmptyStringReturnsUnspecified() {
        assertEquals(APIHashFunction.UNSPECIFIED, APIHashFunction.fromValue(""));
    }

    @Test
    void fromValueNullReturnsUnspecified() {
        assertEquals(APIHashFunction.UNSPECIFIED, APIHashFunction.fromValue(null));
    }

    @Test
    void deserializeSignatureWithEmptyHashFuncDoesNotThrow() throws Exception {
        // Reproduces the Hedera adapter incident: router posted hashFunc="" and Jackson
        // failed inside APIHashFunction.fromValue.
        String json = "{\"signature\":\"0x00\",\"hashFunc\":\"\",\"template\":{\"type\":\"hashList\",\"hash\":\"0x00\",\"hashGroups\":[]}}";
        APISignature sig = new ObjectMapper().readValue(json, APISignature.class);
        assertEquals(APIHashFunction.UNSPECIFIED, sig.getHashFunc());
    }

    @Test
    void fromValueUnknownStillThrows() {
        // Sanity: only empty/null are special-cased; truly unknown values still surface as errors.
        assertThrows(IllegalArgumentException.class, () -> APIHashFunction.fromValue("md5"));
    }
}
