package io.ownera.ledger.adapter.service.asset;

import io.ownera.ledger.adapter.service.model.Asset;
import io.ownera.ledger.adapter.service.model.AssetType;
import io.ownera.ledger.adapter.service.model.LedgerAssetIdentifier;
import io.ownera.ledger.adapter.vanilla.VanillaPostgresHolder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link DbAssetStore} against the shared Testcontainers Postgres (via
 * {@link VanillaPostgresHolder}). The skeleton itself is DB-free at test scope, so the
 * Postgres integration tests live in vanilla-service.
 *
 * <p>Focus is the new {@code getByTokenId} / {@code existsByTokenId} lookup added so adapters
 * honouring {@code AssetBind} during {@code createAsset} can confirm the bound asset is
 * actually registered locally rather than blindly echoing the supplied tokenId back.
 */
class DbAssetStoreTest {

    private final DbAssetStore store = new DbAssetStore(VanillaPostgresHolder.CTX, VanillaPostgresHolder.SCHEMA);

    @Test
    void savedAssetIsRetrievableByTokenId() {
        String assetId = "asset-tokid-" + System.nanoTime();
        String tokenId = "0xtoken-" + System.nanoTime();
        store.save(new Asset(assetId, AssetType.FINP2P,
                new LedgerAssetIdentifier("eip155:1", tokenId, "ERC-20")));

        Asset found = store.getByTokenId(tokenId);
        assertNotNull(found, "asset must be retrievable by the on-chain tokenId it was saved with");
        assertEquals(assetId, found.assetId);
        assertEquals(AssetType.FINP2P, found.assetType);
        assertNotNull(found.ledgerIdentifier);
        assertEquals(tokenId, found.ledgerIdentifier.tokenId);
        assertEquals("eip155:1", found.ledgerIdentifier.network);
        assertEquals("ERC-20", found.ledgerIdentifier.standard);
    }

    @Test
    void existsByTokenIdMirrorsGetByTokenId() {
        String tokenId = "0xtoken-exists-" + System.nanoTime();
        assertFalse(store.existsByTokenId(tokenId), "fresh tokenId must not exist");

        store.save(new Asset("asset-" + System.nanoTime(), AssetType.FINP2P,
                new LedgerAssetIdentifier("net", tokenId, "std")));
        assertTrue(store.existsByTokenId(tokenId), "tokenId must surface after save");
    }

    @Test
    void unknownTokenIdReturnsNullAndFalse() {
        // A tokenId no one ever saved must yield a clean miss — that's the signal adapters use
        // to reject AssetBind references to unregistered assets.
        String tokenId = "never-registered-" + System.nanoTime();
        assertNull(store.getByTokenId(tokenId));
        assertFalse(store.existsByTokenId(tokenId));
    }

    @Test
    void blankTokenIdIsAlwaysAMiss() {
        // The 0.27.x → 0.28 migration backfills existing rows with token_id=''.  We must NOT
        // surface those rows when a caller passes a blank tokenId — that would conflate
        // "unbound legacy row" with "registered asset".  Both null and the empty string are
        // treated as a miss.
        assertNull(store.getByTokenId(null));
        assertNull(store.getByTokenId(""));
        assertFalse(store.existsByTokenId(null));
        assertFalse(store.existsByTokenId(""));
    }

    @Test
    void duplicateTokenIdReturnsOneOfTheMatchesWithoutThrowing() {
        // token_id is not declared UNIQUE at the DB layer (the PK is (type, id)), so an adapter
        // bug could theoretically bind the same on-chain token to two FinP2P assetIds.  The
        // lookup must degrade gracefully — pick one, log loudly, don't throw in the hot path.
        String sharedTokenId = "0xdup-" + System.nanoTime();
        String firstAssetId = "asset-A-" + System.nanoTime();
        String secondAssetId = "asset-B-" + System.nanoTime();
        store.save(new Asset(firstAssetId, AssetType.FINP2P,
                new LedgerAssetIdentifier("net", sharedTokenId, "std")));
        store.save(new Asset(secondAssetId, AssetType.FINP2P,
                new LedgerAssetIdentifier("net", sharedTokenId, "std")));

        Asset found = store.getByTokenId(sharedTokenId);
        assertNotNull(found);
        assertTrue(found.assetId.equals(firstAssetId) || found.assetId.equals(secondAssetId),
                "must return one of the duplicated rows, not throw; got " + found.assetId);
    }

    @Test
    void backCompatGetByIdStillWorksAfterTokenIdAdditions() {
        // Sanity: the new lookup paths can't have regressed the existing primary-key lookup.
        String assetId = "asset-backcompat-" + System.nanoTime();
        store.save(new Asset(assetId, AssetType.FINP2P,
                new LedgerAssetIdentifier("net", "tok-" + System.nanoTime(), "std")));
        assertTrue(store.exists(assetId));
        assertNotNull(store.getById(assetId));
    }
}
