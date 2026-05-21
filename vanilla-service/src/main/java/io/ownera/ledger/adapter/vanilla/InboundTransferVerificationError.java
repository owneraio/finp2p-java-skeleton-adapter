package io.ownera.ledger.adapter.vanilla;

/**
 * Thrown by {@link TransferDelegate#onInboundTransfer} when the on-chain transfer cannot be
 * verified. The vanilla service catches this and skips the local credit so the destination
 * account does not get inflated by an unverifiable claim.
 */
public class InboundTransferVerificationError extends RuntimeException {

    public InboundTransferVerificationError(String message) {
        super(message);
    }

    public InboundTransferVerificationError(String message, Throwable cause) {
        super(message, cause);
    }
}
