package za.co.safintech.payments.payment.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull UUID invoiceId,
        @NotBlank String paymentMethod) {
}
