package io.ownera.ledger.adapter.service.plan;

/**
 * Thrown by an {@link InboundTransferHook} to reject an inbound transfer during plan approval
 * or instruction-level proposal. The framework catches this specifically and converts it to a
 * {@code RejectedPlan} response with the supplied {@code code} / {@code message}.
 *
 * <p>Any other exception escaping a hook is treated as a hook bug — warn-logged and otherwise
 * ignored (the plan is still approved). That distinction matters: it keeps unrelated hook
 * failures from accidentally fail-closing legitimate plans, while giving adapters a typed
 * "deliberate reject" channel.
 */
public class InboundTransferRejection extends RuntimeException {

    private final int code;

    public InboundTransferRejection(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
