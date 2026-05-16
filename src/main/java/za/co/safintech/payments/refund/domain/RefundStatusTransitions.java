package za.co.safintech.payments.refund.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class RefundStatusTransitions {

    private static final Map<RefundStatus, Set<RefundStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(RefundStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(RefundStatus.REQUESTED,
                EnumSet.of(RefundStatus.PROCESSING, RefundStatus.SUCCEEDED, RefundStatus.FAILED, RefundStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(RefundStatus.PROCESSING,
                EnumSet.of(RefundStatus.SUCCEEDED, RefundStatus.FAILED, RefundStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(RefundStatus.SUCCEEDED, EnumSet.noneOf(RefundStatus.class));
        ALLOWED_TRANSITIONS.put(RefundStatus.FAILED, EnumSet.noneOf(RefundStatus.class));
        ALLOWED_TRANSITIONS.put(RefundStatus.CANCELLED, EnumSet.noneOf(RefundStatus.class));
    }

    private RefundStatusTransitions() {
    }

    public static boolean canMove(RefundStatus currentStatus, RefundStatus nextStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(nextStatus);
    }
}
