package za.co.safintech.payments.balance.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MerchantBalanceResponse(
        UUID merchantId,
        String currency,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal refundedAmount,
        BigDecimal availableAmount,
        BigDecimal settledAmount) {
}
