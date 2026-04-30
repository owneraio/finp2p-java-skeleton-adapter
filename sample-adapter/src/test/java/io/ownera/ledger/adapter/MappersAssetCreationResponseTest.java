package io.ownera.ledger.adapter;

import io.ownera.ledger.adapter.api.model.*;
import io.ownera.ledger.adapter.service.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests covering the asset-creation response wire shape on every emission path.
 * Regression coverage for the CAIP-19 discriminator bug (PR #39):
 *   FinP2P node rejects ledgerIdentifier missing assetIdentifierType="CAIP-19".
 *
 * Three paths emit an asset-creation response:
 *   1. Sync createAsset response — Mappers.toAPIResponse(AssetCreationStatus)
 *   2. Operation-status polling — Mappers.toAPI(OperationStatus) wrapping AssetCreationStatus
 *   3. Operational callback — Mappers.toSdkOperationStatus(OperationStatus) (sent via OperationalSDK)
 */
public class MappersAssetCreationResponseTest {

    private static SuccessfulAssetCreation success() {
        return new SuccessfulAssetCreation(new AssetCreationResult(
                "0.0.8818701",
                new LedgerReference("hedera:mainnet", "0.0.0", "HTS-fungible", null)
        ));
    }

    private static void assertCaip19(APILedgerAssetIdentifier id) {
        assertNotNull(id, "ledgerIdentifier must not be null");
        APILedgerAssetIdentifierTypeCAIP19 caip19 = (APILedgerAssetIdentifierTypeCAIP19) id.getActualInstance();
        assertEquals(APILedgerAssetIdentifierTypeCAIP19.AssetIdentifierTypeEnum.CAIP_19,
                caip19.getAssetIdentifierType(), "assetIdentifierType discriminator must be CAIP-19");
        assertEquals("0.0.8818701", caip19.getTokenId(), "tokenId");
        assertEquals("hedera:mainnet", caip19.getNetwork(), "network");
        assertEquals("HTS-fungible", caip19.getStandard(), "standard");
    }

    @Test
    void syncCreateAssetResponseIncludesCaip19Discriminator() {
        APICreateAssetResponse resp = Mappers.toAPIResponse(success());
        assertCaip19(resp.getResponse().getLedgerAssetInfo().getLedgerIdentifier());
    }

    @Test
    void pollingOperationStatusForAssetCreationIncludesCaip19Discriminator() {
        // Upcast forces the OperationStatus overload (the polling path).
        OperationStatus opStatus = success();
        APIOperationStatus status = Mappers.toAPI(opStatus);
        APIOperationStatusCreateAsset wrap = (APIOperationStatusCreateAsset) status.getActualInstance();
        APICreateAssetOperation op = wrap.getOperation();
        assertCaip19(op.getResponse().getLedgerAssetInfo().getLedgerIdentifier());
    }

    @Test
    void sdkCallbackOperationStatusForAssetCreationIncludesCaip19Discriminator() {
        // CallbackClient impls call OperationalSDK.sendCallbackResponse(cid, opapi.model.OperationStatus).
        // Lock in the SDK type's CAIP-19 shape too.
        io.ownera.finp2p.opapi.model.OperationStatus sdkStatus = Mappers.toSdkOperationStatus(success());
        io.ownera.finp2p.opapi.model.OperationStatusCreateAsset wrap =
                (io.ownera.finp2p.opapi.model.OperationStatusCreateAsset) sdkStatus.getActualInstance();
        io.ownera.finp2p.opapi.model.LedgerAssetIdentifierTypeCAIP19 caip19 =
                wrap.getOperation().getResponse().getLedgerAssetInfo().getLedgerIdentifier();
        assertEquals(io.ownera.finp2p.opapi.model.LedgerAssetIdentifierTypeCAIP19.AssetIdentifierTypeEnum.CAIP_19,
                caip19.getAssetIdentifierType(), "SDK assetIdentifierType discriminator must be CAIP-19");
        assertEquals("0.0.8818701", caip19.getTokenId(), "SDK tokenId");
        assertEquals("hedera:mainnet", caip19.getNetwork(), "SDK network");
        assertEquals("HTS-fungible", caip19.getStandard(), "SDK standard");
        assertEquals(io.ownera.finp2p.opapi.model.OperationStatusCreateAsset.TypeEnum.CREATE_ASSET,
                wrap.getType(), "SDK OperationStatusCreateAsset.type must be CREATE_ASSET");
    }
}
