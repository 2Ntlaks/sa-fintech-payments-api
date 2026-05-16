package za.co.safintech.payments.refund.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        UUID merchantId,
        UUID paymentId,
        BigDecimal amount,
        String currency,
        String status,
        String providerReference,
        String reason) {
}
