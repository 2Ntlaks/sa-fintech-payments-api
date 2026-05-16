package za.co.safintech.payments.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import za.co.safintech.payments.merchant.domain.MerchantType;

public record RegisterMerchantRequest(
        @NotBlank @Size(max = 160) String businessName,
        @Size(max = 160) String tradingName,
        @Size(max = 80) String registrationNumber,
        @NotNull MerchantType merchantType,
        @NotBlank @Size(max = 160) String ownerFullName,
        @NotBlank @Email @Size(max = 320) String ownerEmail,
        @NotBlank @Size(min = 8, max = 72) String password) {
}
