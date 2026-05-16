package za.co.safintech.payments.webhook.domain;

public enum WebhookProcessingStatus {
    RECEIVED,
    PROCESSED,
    IGNORED_DUPLICATE,
    IGNORED_OUT_OF_ORDER,
    FAILED_VALIDATION,
    FAILED_PROCESSING
}
