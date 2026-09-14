package io.ownera.ledger.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ownera.ledger.adapter.api.model.APIAccount;
import io.ownera.ledger.adapter.api.model.APIAccountLedgerAccount;
import io.ownera.ledger.adapter.api.model.APICaip10LedgerAccount;
import io.ownera.ledger.adapter.api.model.APICustodialLedgerAccount;
import io.ownera.ledger.adapter.api.model.APINetworkAccount;
import io.ownera.ledger.adapter.api.model.APIWalletAccount;
import io.ownera.ledger.adapter.api.model.APIWalletLedgerAccount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The 0.28 spec closed walletAccount.type to the single value "walletAccount" and widened the
 * ledgerAccount oneOf from one variant to three. Both changes are deserialization hazards that
 * nothing else in the suite exercises — no existing test posts a ledgerAccount body at all.
 *
 * The oneOf deserializers are match-count based: they require exactly one variant to match and
 * throw otherwise, so a value that matches zero variants fails the whole request, not just the
 * field.
 */
public class LedgerAccountBindingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static String account(String ledgerAccountJson) {
        return "{\"finId\":\"finid-1\",\"ledgerAccount\":" + ledgerAccountJson + "}";
    }

    @Test
    void acceptsCurrentWalletAccountSpelling() throws Exception {
        APIAccount acc = mapper.readValue(
                account("{\"type\":\"walletAccount\",\"address\":\"0xabc\"}"), APIAccount.class);

        Object actual = acc.getLedgerAccount().getActualInstance();
        assertInstanceOf(APIWalletLedgerAccount.class, actual);
        assertEquals("0xabc", ((APIWalletLedgerAccount) actual).getAddress());
    }

    /**
     * This adapter emitted type:"wallet" until 0.28. The spec no longer lists that value, so
     * without the compatibility alias its own previously-emitted payloads match zero variants
     * and the request 400s.
     */
    @Test
    void acceptsLegacyWalletSpelling() throws Exception {
        APIAccount acc = mapper.readValue(
                account("{\"type\":\"wallet\",\"address\":\"0xabc\"}"), APIAccount.class);

        Object actual = acc.getLedgerAccount().getActualInstance();
        assertInstanceOf(APIWalletLedgerAccount.class, actual);
        assertEquals(APIWalletLedgerAccount.TypeEnum.WALLETACCOUNT,
                ((APIWalletLedgerAccount) actual).getType());
    }

    @Test
    void legacyWalletSpellingMapsToEnum() {
        assertEquals(APIWalletLedgerAccount.TypeEnum.WALLETACCOUNT,
                APIWalletLedgerAccount.TypeEnum.fromValue("wallet"));
        assertEquals(APIWalletLedgerAccount.TypeEnum.WALLETACCOUNT,
                APIWalletLedgerAccount.TypeEnum.fromValue("walletAccount"));
    }

    @Test
    void unknownWalletTypeStillRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> APIWalletLedgerAccount.TypeEnum.fromValue("definitely-not-a-wallet"));
    }

    @Test
    void bindsCaip10Variant() throws Exception {
        APIAccount acc = mapper.readValue(
                account("{\"type\":\"caip10Account\",\"network\":\"eip155:1\",\"address\":\"0xabc\"}"),
                APIAccount.class);

        Object actual = acc.getLedgerAccount().getActualInstance();
        assertInstanceOf(APICaip10LedgerAccount.class, actual);
        assertEquals("eip155:1", ((APICaip10LedgerAccount) actual).getNetwork());
    }

    @Test
    void bindsCustodialVariant() throws Exception {
        APIAccount acc = mapper.readValue(
                account("{\"type\":\"custodialAccount\",\"provider\":\"fireblocks\","
                        + "\"vaultAccountId\":\"v1\",\"assetId\":\"ETH\"}"),
                APIAccount.class);

        assertInstanceOf(APICustodialLedgerAccount.class,
                acc.getLedgerAccount().getActualInstance());
    }

    @Test
    void walletEnvelopeSerializesWithCurrentSpelling() throws Exception {
        String json = mapper.writeValueAsString(new APIAccountLedgerAccount(
                new APIWalletLedgerAccount()
                        .type(APIWalletLedgerAccount.TypeEnum.WALLETACCOUNT)
                        .address("0xabc")));

        assertTrue(json.contains("\"type\":\"walletAccount\""), json);
    }

    /**
     * noneAccount is an empty-object schema, which the generator renders as a bare Object oneOf
     * variant. Unguarded, that matches every JSON object, so the match count is always >= 2 and
     * APINetworkAccount throws for all input.
     */
    @Test
    void networkAccountResolvesConcreteVariantDespiteEmptyObjectSchema() throws Exception {
        APINetworkAccount acc = mapper.readValue(
                "{\"type\":\"walletAccount\",\"address\":\"0xabc\"}", APINetworkAccount.class);

        assertInstanceOf(APIWalletAccount.class, acc.getActualInstance());
    }

    @Test
    void networkAccountAcceptsEmptyObjectAsNoneAccount() throws Exception {
        APINetworkAccount acc = mapper.readValue("{}", APINetworkAccount.class);

        assertNotNull(acc.getActualInstance());
    }
}
