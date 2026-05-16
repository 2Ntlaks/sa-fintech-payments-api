package za.co.safintech.payments.merchant.dto;

import java.util.UUID;

public record MerchantProfileResponse(
        UUID merchantId,
        String businessName,
        String tradingName,
        String merchantType,
        String status,
        String countryCode,
        String defaultCurrency,
        UUID merchantUserId,
        String userRole) {
}
