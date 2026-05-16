package za.co.safintech.payments.refund.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRefundRequest(
        @NotNull UUID paymentId,
        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @Size(max = 255) String reason) {
}
