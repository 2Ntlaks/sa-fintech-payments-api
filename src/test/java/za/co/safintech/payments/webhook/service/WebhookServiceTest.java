package za.co.safintech.payments.webhook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import za.co.safintech.payments.audit.service.AuditLogService;
import za.co.safintech.payments.balance.service.FeeCalculator;
import za.co.safintech.payments.balance.service.MerchantBalanceService;
import za.co.safintech.payments.common.exception.ApiException;
import za.co.safintech.payments.customer.domain.Customer;
import za.co.safintech.payments.invoice.domain.Invoice;
import za.co.safintech.payments.merchant.domain.Merchant;
import za.co.safintech.payments.merchant.domain.MerchantType;
import za.co.safintech.payments.payment.domain.PaymentAttempt;
import za.co.safintech.payments.payment.domain.PaymentMethod;
import za.co.safintech.payments.payment.domain.PaymentStatus;
import za.co.safintech.payments.payment.repository.PaymentAttemptRepository;
import za.co.safintech.payments.webhook.domain.WebhookEvent;
import za.co.safintech.payments.webhook.dto.SimulatedPaymentWebhookRequest;
import za.co.safintech.payments.webhook.repository.WebhookEventRepository;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private MerchantBalanceService merchantBalanceService;

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(
                webhookEventRepository,
                paymentAttemptRepository,
                auditLogService,
                new FeeCalculator(),
                merchantBalanceService,
                new ObjectMapper(),
                "test-secret");
    }

    @Test
    void shouldProcessPaymentStatusWebhookOnce() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        Invoice invoice = issuedInvoice(merchant);
        PaymentAttempt paymentAttempt = new PaymentAttempt(merchant, invoice, "SIM-PAY-123", PaymentMethod.CARD_SIMULATED);
        paymentAttempt.transitionTo(PaymentStatus.PROCESSING, null);
        SimulatedPaymentWebhookRequest request = new SimulatedPaymentWebhookRequest(
                "evt-1", "payment.status_changed", "SIM-PAY-123", "SUCCEEDED", null);

        when(webhookEventRepository.findByProviderEventId("evt-1")).thenReturn(Optional.empty());
        when(paymentAttemptRepository.findWithLockByProviderReference("SIM-PAY-123")).thenReturn(Optional.of(paymentAttempt));
        when(paymentAttemptRepository.existsByInvoiceIdAndStatusIn(invoice.id(), successfulPaymentStatuses())).thenReturn(false);
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = webhookService.processPaymentWebhook("test-secret", request);

        assertThat(response.processingStatus()).isEqualTo("PROCESSED");
        assertThat(response.paymentId()).isEqualTo(paymentAttempt.id());
        assertThat(paymentAttempt.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(paymentAttempt.feeAmount()).isEqualByComparingTo("4.64");
        assertThat(paymentAttempt.netAmount()).isEqualByComparingTo("120.86");
        assertThat(invoice.status().name()).isEqualTo("PAID");
        verify(merchantBalanceService).applySuccessfulPayment(
                merchant.id(),
                new BigDecimal("125.50"),
                new BigDecimal("4.64"),
                new BigDecimal("120.86"));
        verify(auditLogService).record(merchant.id(), null, "WEBHOOK_PAYMENT_STATUS_PROCESSED",
                "WEBHOOK_EVENT", response.id(), "PROCESSING", "SUCCEEDED");
    }

    @Test
    void shouldReturnDuplicateDecisionForRepeatedProviderEventId() {
        WebhookEvent existingEvent = new WebhookEvent(
                "evt-1", "payment.status_changed", "SIM-PAY-123", "SUCCEEDED", "{}");
        existingEvent.complete(null, null, za.co.safintech.payments.webhook.domain.WebhookProcessingStatus.FAILED_VALIDATION,
                "Payment provider reference was not found");
        SimulatedPaymentWebhookRequest request = new SimulatedPaymentWebhookRequest(
                "evt-1", "payment.status_changed", "SIM-PAY-123", "SUCCEEDED", null);

        when(webhookEventRepository.findByProviderEventId("evt-1")).thenReturn(Optional.of(existingEvent));

        var response = webhookService.processPaymentWebhook("test-secret", request);

        assertThat(response.processingStatus()).isEqualTo("IGNORED_DUPLICATE");
        assertThat(response.failureReason()).isEqualTo("Payment provider reference was not found");
    }

    @Test
    void shouldIgnoreOutOfOrderWebhookThatMovesPaymentBackward() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        Invoice invoice = issuedInvoice(merchant);
        PaymentAttempt paymentAttempt = new PaymentAttempt(merchant, invoice, "SIM-PAY-123", PaymentMethod.CARD_SIMULATED);
        paymentAttempt.transitionTo(PaymentStatus.PROCESSING, null);
        paymentAttempt.transitionTo(PaymentStatus.SUCCEEDED, null);
        invoice.markPaid();
        SimulatedPaymentWebhookRequest request = new SimulatedPaymentWebhookRequest(
                "evt-2", "payment.status_changed", "SIM-PAY-123", "FAILED", "Late failure event");

        when(webhookEventRepository.findByProviderEventId("evt-2")).thenReturn(Optional.empty());
        when(paymentAttemptRepository.findWithLockByProviderReference("SIM-PAY-123")).thenReturn(Optional.of(paymentAttempt));
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = webhookService.processPaymentWebhook("test-secret", request);

        assertThat(response.processingStatus()).isEqualTo("IGNORED_OUT_OF_ORDER");
        assertThat(paymentAttempt.status()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void shouldStoreFailedValidationForUnsupportedWebhookPaymentStatus() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        Invoice invoice = issuedInvoice(merchant);
        PaymentAttempt paymentAttempt = new PaymentAttempt(merchant, invoice, "SIM-PAY-123", PaymentMethod.CARD_SIMULATED);
        SimulatedPaymentWebhookRequest request = new SimulatedPaymentWebhookRequest(
                "evt-invalid", "payment.status_changed", "SIM-PAY-123", "BOGUS", null);

        when(webhookEventRepository.findByProviderEventId("evt-invalid")).thenReturn(Optional.empty());
        when(paymentAttemptRepository.findWithLockByProviderReference("SIM-PAY-123")).thenReturn(Optional.of(paymentAttempt));
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = webhookService.processPaymentWebhook("test-secret", request);

        assertThat(response.processingStatus()).isEqualTo("FAILED_VALIDATION");
        assertThat(response.paymentId()).isEqualTo(paymentAttempt.id());
        assertThat(paymentAttempt.status()).isEqualTo(PaymentStatus.CREATED);
    }

    @Test
    void shouldRejectInvalidWebhookSecret() {
        SimulatedPaymentWebhookRequest request = new SimulatedPaymentWebhookRequest(
                "evt-1", "payment.status_changed", "SIM-PAY-123", "SUCCEEDED", null);

        assertThatThrownBy(() -> webhookService.processPaymentWebhook("wrong-secret", request))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(apiException.code()).isEqualTo("INVALID_WEBHOOK_SECRET");
                });
    }

    private Invoice issuedInvoice(Merchant merchant) {
        Customer customer = new Customer(merchant, "Lerato Khumalo", "lerato@example.com", "+27821234567");
        return new Invoice(merchant, customer, "INV-TEST123", "Grocery order", new BigDecimal("125.50"), null);
    }

    private List<PaymentStatus> successfulPaymentStatuses() {
        return List.of(PaymentStatus.SUCCEEDED, PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED);
    }
}
