package za.co.safintech.payments.webhook.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    private UUID id;

    private UUID merchantId;

    @Column(nullable = false, length = 120)
    private String providerEventId;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Column(length = 64)
    private String providerReference;

    private UUID targetPaymentId;

    @Column(length = 40)
    private String requestedPaymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WebhookProcessingStatus processingStatus;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(length = 255)
    private String failureReason;

    private Instant processedAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected WebhookEvent() {
    }

    public WebhookEvent(String providerEventId, String eventType, String providerReference,
            String requestedPaymentStatus, String rawPayload) {
        this.id = UUID.randomUUID();
        this.providerEventId = providerEventId;
        this.eventType = eventType;
        this.providerReference = providerReference;
        this.requestedPaymentStatus = requestedPaymentStatus;
        this.processingStatus = WebhookProcessingStatus.RECEIVED;
        this.rawPayload = rawPayload;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public void complete(UUID merchantId, UUID targetPaymentId, WebhookProcessingStatus status, String failureReason) {
        this.merchantId = merchantId;
        this.targetPaymentId = targetPaymentId;
        this.processingStatus = status;
        this.failureReason = failureReason;
        this.processedAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public UUID merchantId() {
        return merchantId;
    }

    public String providerEventId() {
        return providerEventId;
    }

    public WebhookProcessingStatus processingStatus() {
        return processingStatus;
    }

    public UUID targetPaymentId() {
        return targetPaymentId;
    }

    public String failureReason() {
        return failureReason;
    }
}
