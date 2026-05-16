package za.co.safintech.payments.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import za.co.safintech.payments.invoice.repository.InvoiceRepository;
import za.co.safintech.payments.merchant.domain.Merchant;
import za.co.safintech.payments.merchant.domain.MerchantType;
import za.co.safintech.payments.payment.domain.PaymentAttempt;
import za.co.safintech.payments.payment.domain.PaymentMethod;
import za.co.safintech.payments.payment.domain.PaymentStatus;
import za.co.safintech.payments.payment.dto.CreatePaymentRequest;
import za.co.safintech.payments.payment.dto.UpdatePaymentStatusRequest;
import za.co.safintech.payments.payment.domain.IdempotencyRecord;
import za.co.safintech.payments.payment.repository.IdempotencyRecordRepository;
import za.co.safintech.payments.payment.repository.PaymentAttemptRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private MerchantBalanceService merchantBalanceService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentAttemptRepository, invoiceRepository, idempotencyRecordRepository,
                auditLogService, new FeeCalculator(), merchantBalanceService);
    }

    @Test
    void shouldCreatePaymentAttemptForMerchantOwnedIssuedInvoice() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        Invoice invoice = issuedInvoice(merchant);
        UUID actorId = UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(invoice.id(), "PAYSHAP_SIMULATED");

        when(invoiceRepository.findByIdAndMerchantId(invoice.id(), merchant.id())).thenReturn(Optional.of(invoice));
        when(paymentAttemptRepository.save(any(PaymentAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPaymentAttempt(merchant.id(), actorId, request, null);

        assertThat(response.merchantId()).isEqualTo(merchant.id());
        assertThat(response.invoiceId()).isEqualTo(invoice.id());
        assertThat(response.amount()).isEqualByComparingTo("125.50");
        assertThat(response.grossAmount()).isEqualByComparingTo("125.50");
        assertThat(response.feeAmount()).isEqualByComparingTo("0.00");
        assertThat(response.netAmount()).isEqualByComparingTo("125.50");
        assertThat(response.currency()).isEqualTo("ZAR");
        assertThat(response.paymentMethod()).isEqualTo("PAYSHAP_SIMULATED");
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.providerReference()).startsWith("SIM-PAY-");
        verify(auditLogService).record(merchant.id(), actorId, "PAYMENT_ATTEMPT_CREATED", "PAYMENT_ATTEMPT",
                response.id(), null, "CREATED");
    }

    @Test
    void shouldReturnOriginalPaymentWhenIdempotencyKeyIsRetriedWithSameRequest() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        Invoice invoice = issuedInvoice(merchant);
        PaymentAttempt paymentAttempt = new PaymentAttempt(merchant, invoice, "SIM-PAY-123", PaymentMethod.PAYSHAP_SIMULATED);
        UUID actorId = UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(invoice.id(), "PAYSHAP_SIMULATED");

        when(idempotencyRecordRepository.findByMerchantIdAndOperationAndIdempotencyKey(
                merchant.id(), "CREATE_PAYMENT_ATTEMPT", "retry-key-1"))
                .thenReturn(Optional.of(new IdempotencyRecord(
                        merchant.id(),
                        "CREATE_PAYMENT_ATTEMPT",
                        "retry-key-1",
                        requestHash(request),
                        "PAYMENT_ATTEMPT",
                        paymentAttempt.id())));
        when(paymentAttemptRepository.findByIdAndMerchantId(paymentAttempt.id(), merchant.id()))
                .thenReturn(Optional.of(paymentAttempt));

        var response = paymentService.createPaymentAttempt(merchant.id(), actorId, request, "retry-key-1");

        assertThat(response.id()).isEqualTo(paymentAttempt.id());
        assertThat(response.providerReference()).isEqualTo("SIM-PAY-123");
    }

    @Test
    void shouldRejectIdempotencyKeyReusedWithDifferentPaymentRequest() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        UUID actorId = UUID.randomUUID();
        UUID originalPaymentId = UUID.randomUUID();
        CreatePaymentRequest request = new CreatePaymentRequest(UUID.randomUUID(), "CARD_SIMULATED");

        when(idempotencyRecordRepository.findByMerchantIdAndOperationAndIdempotencyKey(
                merchant.id(), "CREATE_PAYMENT_ATTEMPT", "retry-key-1"))
                .thenReturn(Optional.of(new IdempotencyRecord(
                        merchant.id(),
                        "CREATE_PAYMENT_ATTEMPT",
                        "retry-key-1",
                        "different-hash",
                        "PAYMENT_ATTEMPT",
                        originalPaymentId)));

        assertThatThrownBy(() -> paymentService.createPaymentAttempt(merchant.id(), actorId, request, "retry-key-1"))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.code()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
                });
    }

    @Test
    void shouldRejectPaymentForInvoiceOutsideAuthenticatedMerchant() {
        UUID merchantId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        when(invoiceRepository.findByIdAndMerchantId(invoiceId, merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPaymentAttempt(merchantId, UUID.randomUUID(),
                        new CreatePaymentRequest(invoiceId, "CARD_SIMULATED"), null))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.code()).isEqualTo("INVOICE_NOT_FOUND");
                });
    }

    @Test
    void shouldRejectPaymentAgainstCancelledInvoice() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        Invoice invoice = issuedInvoice(merchant);
        invoice.cancel();

        when(invoiceRepository.findByIdAndMerchantId(invoice.id(), merchant.id())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> paymentService.createPaymentAttempt(merchant.id(), UUID.randomUUID(),
                        new CreatePaymentRequest(invoice.id(), "CARD_SIMULATED"), null))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.code()).isEqualTo("INVOICE_NOT_PAYABLE");
                });
    }

    @Test
    void shouldRejectUnsupportedPaymentMethod() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        Invoice invoice = issuedInvoice(merchant);

        when(invoiceRepository.findByIdAndMerchantId(invoice.id(), merchant.id())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> paymentService.createPaymentAttempt(merchant.id(), UUID.randomUUID(),
                        new CreatePaymentRequest(invoice.id(), "REAL_BANK_TRANSFER"), null))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.code()).isEqualTo("UNSUPPORTED_PAYMENT_METHOD");
                });
    }

    @Test
    void shouldMarkInvoicePaidWhenPaymentSucceeds() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        Invoice invoice = issuedInvoice(merchant);
        PaymentAttempt paymentAttempt = new PaymentAttempt(merchant, invoice, "SIM-PAY-123", PaymentMethod.CARD_SIMULATED);
        paymentAttempt.transitionTo(PaymentStatus.PROCESSING, null);
        UUID actorId = UUID.randomUUID();

        when(paymentAttemptRepository.findWithLockByIdAndMerchantId(paymentAttempt.id(), merchant.id()))
                .thenReturn(Optional.of(paymentAttempt));
        when(paymentAttemptRepository.existsByInvoiceIdAndStatusIn(invoice.id(), successfulPaymentStatuses())).thenReturn(false);

        var response = paymentService.updatePaymentStatus(merchant.id(), actorId, paymentAttempt.id(),
                new UpdatePaymentStatusRequest("SUCCEEDED", null));

        assertThat(response.status()).isEqualTo("SUCCEEDED");
        assertThat(response.grossAmount()).isEqualByComparingTo("125.50");
        assertThat(response.feeAmount()).isEqualByComparingTo("4.64");
        assertThat(response.netAmount()).isEqualByComparingTo("120.86");
        assertThat(invoice.status().name()).isEqualTo("PAID");
        verify(merchantBalanceService).applySuccessfulPayment(
                merchant.id(),
                new BigDecimal("125.50"),
                new BigDecimal("4.64"),
                new BigDecimal("120.86"));
        verify(auditLogService).record(merchant.id(), actorId, "PAYMENT_STATUS_CHANGED", "PAYMENT_ATTEMPT",
                paymentAttempt.id(), "PROCESSING", "SUCCEEDED");
    }

    @Test
    void shouldRejectDuplicateSuccessfulPaymentForSameInvoice() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        Invoice invoice = issuedInvoice(merchant);
        PaymentAttempt paymentAttempt = new PaymentAttempt(merchant, invoice, "SIM-PAY-123", PaymentMethod.CARD_SIMULATED);
        paymentAttempt.transitionTo(PaymentStatus.PROCESSING, null);

        when(paymentAttemptRepository.findWithLockByIdAndMerchantId(paymentAttempt.id(), merchant.id()))
                .thenReturn(Optional.of(paymentAttempt));
        when(paymentAttemptRepository.existsByInvoiceIdAndStatusIn(invoice.id(), successfulPaymentStatuses())).thenReturn(true);

        assertThatThrownBy(() -> paymentService.updatePaymentStatus(merchant.id(), UUID.randomUUID(), paymentAttempt.id(),
                        new UpdatePaymentStatusRequest("SUCCEEDED", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.code()).isEqualTo("INVOICE_ALREADY_PAID");
                });
    }

    @Test
    void shouldRejectInvalidPaymentStatusTransition() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        Invoice invoice = issuedInvoice(merchant);
        PaymentAttempt paymentAttempt = new PaymentAttempt(merchant, invoice, "SIM-PAY-123", PaymentMethod.CARD_SIMULATED);
        paymentAttempt.transitionTo(PaymentStatus.PROCESSING, null);
        paymentAttempt.transitionTo(PaymentStatus.FAILED, "Insufficient funds");

        when(paymentAttemptRepository.findWithLockByIdAndMerchantId(paymentAttempt.id(), merchant.id()))
                .thenReturn(Optional.of(paymentAttempt));

        assertThatThrownBy(() -> paymentService.updatePaymentStatus(merchant.id(), UUID.randomUUID(), paymentAttempt.id(),
                        new UpdatePaymentStatusRequest("SUCCEEDED", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.code()).isEqualTo("INVALID_PAYMENT_STATUS_TRANSITION");
                });
    }

    private Invoice issuedInvoice(Merchant merchant) {
        Customer customer = new Customer(merchant, "Lerato Khumalo", "lerato@example.com", "+27821234567");
        return new Invoice(merchant, customer, "INV-TEST123", "Grocery order", new BigDecimal("125.50"), null);
    }

    private String requestHash(CreatePaymentRequest request) {
        String fingerprint = request.invoiceId() + "|" + request.paymentMethod().toUpperCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fingerprint.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private List<PaymentStatus> successfulPaymentStatuses() {
        return List.of(PaymentStatus.SUCCEEDED, PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED);
    }
}
