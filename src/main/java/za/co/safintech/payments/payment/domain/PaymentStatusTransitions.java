package za.co.safintech.payments.payment.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class PaymentStatusTransitions {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(PaymentStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(PaymentStatus.CREATED,
                EnumSet.of(PaymentStatus.PENDING, PaymentStatus.PROCESSING, PaymentStatus.CANCELLED, PaymentStatus.EXPIRED));
        ALLOWED_TRANSITIONS.put(PaymentStatus.PENDING,
                EnumSet.of(PaymentStatus.PROCESSING, PaymentStatus.SUCCEEDED, PaymentStatus.FAILED, PaymentStatus.CANCELLED, PaymentStatus.EXPIRED));
        ALLOWED_TRANSITIONS.put(PaymentStatus.PROCESSING,
                EnumSet.of(PaymentStatus.SUCCEEDED, PaymentStatus.FAILED, PaymentStatus.EXPIRED));
        ALLOWED_TRANSITIONS.put(PaymentStatus.SUCCEEDED, EnumSet.noneOf(PaymentStatus.class));
        ALLOWED_TRANSITIONS.put(PaymentStatus.FAILED, EnumSet.noneOf(PaymentStatus.class));
        ALLOWED_TRANSITIONS.put(PaymentStatus.CANCELLED, EnumSet.noneOf(PaymentStatus.class));
        ALLOWED_TRANSITIONS.put(PaymentStatus.EXPIRED, EnumSet.noneOf(PaymentStatus.class));
    }

    private PaymentStatusTransitions() {
    }

    public static boolean canMove(PaymentStatus currentStatus, PaymentStatus nextStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(nextStatus);
    }
}
