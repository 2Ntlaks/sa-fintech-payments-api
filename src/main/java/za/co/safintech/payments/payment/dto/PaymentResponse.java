package za.co.safintech.payments.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID merchantId,
        UUID invoiceId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String status,
        String providerReference,
        String failureReason) {
}
