package za.co.safintech.payments.webhook.service;

import java.util.Locale;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import za.co.safintech.payments.audit.service.AuditLogService;
import za.co.safintech.payments.balance.service.FeeCalculator;
import za.co.safintech.payments.balance.service.MerchantBalanceService;
import za.co.safintech.payments.common.exception.ApiException;
import za.co.safintech.payments.payment.domain.PaymentAttempt;
import za.co.safintech.payments.payment.domain.PaymentStatus;
import za.co.safintech.payments.payment.repository.PaymentAttemptRepository;
import za.co.safintech.payments.webhook.domain.WebhookEvent;
import za.co.safintech.payments.webhook.domain.WebhookProcessingStatus;
import za.co.safintech.payments.webhook.dto.SimulatedPaymentWebhookRequest;
import za.co.safintech.payments.webhook.dto.WebhookEventResponse;
import za.co.safintech.payments.webhook.repository.WebhookEventRepository;

@Service
@Profile("!local")
public class WebhookService {

    private static final List<PaymentStatus> SUCCESSFUL_INVOICE_PAYMENT_STATUSES = List.of(
            PaymentStatus.SUCCEEDED,
            PaymentStatus.PARTIALLY_REFUNDED,
            PaymentStatus.REFUNDED);

    private final WebhookEventRepository webhookEventRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final AuditLogService auditLogService;
    private final FeeCalculator feeCalculator;
    private final MerchantBalanceService merchantBalanceService;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public WebhookService(WebhookEventRepository webhookEventRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            AuditLogService auditLogService,
            FeeCalculator feeCalculator,
            MerchantBalanceService merchantBalanceService,
            ObjectMapper objectMapper,
            @Value("${app.webhooks.simulated.secret}") String webhookSecret) {
        this.webhookEventRepository = webhookEventRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.auditLogService = auditLogService;
        this.feeCalculator = feeCalculator;
        this.merchantBalanceService = merchantBalanceService;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public WebhookEventResponse processPaymentWebhook(String providedSecret, SimulatedPaymentWebhookRequest request) {
        if (!webhookSecret.equals(providedSecret)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SECRET", "Invalid simulated webhook secret");
        }

        return webhookEventRepository.findByProviderEventId(request.providerEventId())
                .map(this::duplicateResponse)
                .orElseGet(() -> processNewEvent(request));
    }

    private WebhookEventResponse processNewEvent(SimulatedPaymentWebhookRequest request) {
        WebhookEvent event = new WebhookEvent(
                request.providerEventId(),
                request.eventType(),
                request.providerReference(),
                request.paymentStatus(),
                rawPayload(request));

        PaymentAttempt paymentAttempt = paymentAttemptRepository.findWithLockByProviderReference(request.providerReference())
                .orElse(null);

        if (paymentAttempt == null) {
            event.complete(null, null, WebhookProcessingStatus.FAILED_VALIDATION, "Payment provider reference was not found");
            WebhookEvent saved = webhookEventRepository.save(event);
            return toResponse(saved);
        }

        PaymentStatus requestedStatus;
        try {
            requestedStatus = parsePaymentStatus(request.paymentStatus());
        } catch (ApiException exception) {
            event.complete(paymentAttempt.merchant().id(), paymentAttempt.id(),
                    WebhookProcessingStatus.FAILED_VALIDATION, exception.getMessage());
            WebhookEvent saved = webhookEventRepository.save(event);
            auditLogService.record(paymentAttempt.merchant().id(), null, "WEBHOOK_PAYMENT_STATUS_REJECTED",
                    "WEBHOOK_EVENT", saved.id(), paymentAttempt.status().name(), request.paymentStatus());
            return toResponse(saved);
        }

        PaymentStatus previousStatus = paymentAttempt.status();
        try {
            if (requestedStatus == PaymentStatus.SUCCEEDED
                    && previousStatus != PaymentStatus.SUCCEEDED
                    && paymentAttemptRepository.existsByInvoiceIdAndStatusIn(
                            paymentAttempt.invoice().id(), SUCCESSFUL_INVOICE_PAYMENT_STATUSES)) {
                throw new ApiException(HttpStatus.CONFLICT, "INVOICE_ALREADY_PAID", "Invoice already has a successful payment");
            }

            if (previousStatus != requestedStatus) {
                paymentAttempt.transitionTo(requestedStatus, request.failureReason());
                if (requestedStatus == PaymentStatus.SUCCEEDED) {
                    paymentAttempt.applySuccessfulFee(feeCalculator.calculateFee(paymentAttempt.grossAmount()));
                    paymentAttempt.invoice().markPaid();
                    merchantBalanceService.applySuccessfulPayment(
                            paymentAttempt.merchant().id(),
                            paymentAttempt.grossAmount(),
                            paymentAttempt.feeAmount(),
                            paymentAttempt.netAmount());
                }
            }
            event.complete(paymentAttempt.merchant().id(), paymentAttempt.id(), WebhookProcessingStatus.PROCESSED, null);
            WebhookEvent saved = webhookEventRepository.save(event);
            auditLogService.record(paymentAttempt.merchant().id(), null, "WEBHOOK_PAYMENT_STATUS_PROCESSED",
                    "WEBHOOK_EVENT", saved.id(), previousStatus.name(), requestedStatus.name());
            return toResponse(saved);
        } catch (IllegalStateException exception) {
            event.complete(paymentAttempt.merchant().id(), paymentAttempt.id(),
                    WebhookProcessingStatus.IGNORED_OUT_OF_ORDER, exception.getMessage());
            WebhookEvent saved = webhookEventRepository.save(event);
            auditLogService.record(paymentAttempt.merchant().id(), null, "WEBHOOK_PAYMENT_STATUS_IGNORED",
                    "WEBHOOK_EVENT", saved.id(), previousStatus.name(), requestedStatus.name());
            return toResponse(saved);
        } catch (ApiException exception) {
            event.complete(paymentAttempt.merchant().id(), paymentAttempt.id(),
                    WebhookProcessingStatus.FAILED_PROCESSING, exception.getMessage());
            WebhookEvent saved = webhookEventRepository.save(event);
            auditLogService.record(paymentAttempt.merchant().id(), null, "WEBHOOK_PAYMENT_STATUS_FAILED",
                    "WEBHOOK_EVENT", saved.id(), previousStatus.name(), requestedStatus.name());
            return toResponse(saved);
        }
    }

    private WebhookEventResponse duplicateResponse(WebhookEvent existingEvent) {
        if (existingEvent.merchantId() != null) {
            auditLogService.record(existingEvent.merchantId(), null, "WEBHOOK_DUPLICATE_IGNORED",
                    "WEBHOOK_EVENT", existingEvent.id(), existingEvent.processingStatus().name(), "IGNORED_DUPLICATE");
        }
        return new WebhookEventResponse(
                existingEvent.id(),
                existingEvent.providerEventId(),
                WebhookProcessingStatus.IGNORED_DUPLICATE.name(),
                existingEvent.targetPaymentId(),
                existingEvent.failureReason());
    }

    private WebhookEventResponse toResponse(WebhookEvent event) {
        return new WebhookEventResponse(
                event.id(),
                event.providerEventId(),
                event.processingStatus().name(),
                event.targetPaymentId(),
                event.failureReason());
    }

    private PaymentStatus parsePaymentStatus(String status) {
        try {
            return PaymentStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PAYMENT_STATUS", "Unsupported payment status");
        }
    }

    private String rawPayload(SimulatedPaymentWebhookRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WEBHOOK_PAYLOAD_NOT_SERIALIZABLE", "Webhook payload could not be stored");
        }
    }
}
