package za.co.safintech.payments.webhook.dto;

import jakarta.validation.constraints.NotBlank;

public record SimulatedPaymentWebhookRequest(
        @NotBlank String providerEventId,
        @NotBlank String eventType,
        @NotBlank String providerReference,
        @NotBlank String paymentStatus,
        String failureReason) {
}
