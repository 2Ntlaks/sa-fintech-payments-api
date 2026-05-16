package za.co.safintech.payments.reconciliation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MockProviderPaymentRecord(
        @NotBlank
        @Size(max = 64)
        String providerReference,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @NotBlank
        @Size(min = 3, max = 3)
        String currency,

        @NotBlank
        @Size(max = 40)
        String status) {
}
