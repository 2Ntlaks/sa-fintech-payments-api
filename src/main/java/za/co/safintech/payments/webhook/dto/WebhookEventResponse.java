package za.co.safintech.payments.webhook.dto;

import java.util.UUID;

public record WebhookEventResponse(
        UUID id,
        String providerEventId,
        String processingStatus,
        UUID paymentId,
        String failureReason) {
}
