package za.co.safintech.payments.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePaymentStatusRequest(
        @NotBlank String status,
        @Size(max = 255) String failureReason) {
}
