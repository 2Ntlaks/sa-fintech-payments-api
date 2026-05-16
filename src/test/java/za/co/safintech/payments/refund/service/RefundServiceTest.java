package za.co.safintech.payments.refund.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import za.co.safintech.payments.audit.service.AuditLogService;
import za.co.safintech.payments.balance.service.MerchantBalanceService;
import za.co.safintech.payments.common.exception.ApiException;
import za.co.safintech.payments.customer.domain.Customer;
import za.co.safintech.payments.invoice.domain.Invoice;
import za.co.safintech.payments.invoice.domain.InvoiceStatus;
import za.co.safintech.payments.merchant.domain.Merchant;
import za.co.safintech.payments.merchant.domain.MerchantType;
import za.co.safintech.payments.payment.domain.PaymentAttempt;
import za.co.safintech.payments.payment.domain.PaymentMethod;
import za.co.safintech.payments.payment.domain.PaymentStatus;
import za.co.safintech.payments.payment.repository.PaymentAttemptRepository;
import za.co.safintech.payments.refund.domain.Refund;
import za.co.safintech.payments.refund.domain.RefundStatus;
import za.co.safintech.payments.refund.dto.CreateRefundRequest;
import za.co.safintech.payments.refund.repository.RefundRepository;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private MerchantBalanceService merchantBalanceService;

    private RefundService refundService;

    @BeforeEach
    void setUp() {
        refundService = new RefundService(refundRepository, paymentAttemptRepository, auditLogService, merchantBalanceService);
    }

    @Test
    void shouldCreatePartialRefundForSuccessfulPayment() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        PaymentAttempt paymentAttempt = successfulPayment(merchant);
        UUID actorId = UUID.randomUUID();

        when(paymentAttemptRepository.findWithLockByIdAndMerchantId(paymentAttempt.id(), merchant.id()))
                .thenReturn(Optional.of(paymentAttempt));
        when(refundRepository.sumAmountByPaymentAttemptIdAndStatus(paymentAttempt.id(), RefundStatus.SUCCEEDED))
                .thenReturn(new BigDecimal("0.00"));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = refundService.createRefund(merchant.id(), actorId,
                new CreateRefundRequest(paymentAttempt.id(), new BigDecimal("50.00"), "Customer returned one item"));

        assertThat(response.amount()).isEqualByComparingTo("50.00");
        assertThat(response.status()).isEqualTo("SUCCEEDED");
        assertThat(response.providerReference()).startsWith("SIM-REF-");
        assertThat(paymentAttempt.status()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(paymentAttempt.invoice().status()).isEqualTo(InvoiceStatus.PARTIALLY_REFUNDED);
        verify(merchantBalanceService).applySuccessfulRefund(merchant.id(), new BigDecimal("50.00"));
        verify(auditLogService).record(merchant.id(), actorId, "REFUND_SUCCEEDED", "REFUND",
                response.id(), "REQUESTED", "SUCCEEDED");
    }

    @Test
    void shouldCreateFullRefundAfterPreviousPartialRefund() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        PaymentAttempt paymentAttempt = successfulPayment(merchant);
        paymentAttempt.applySuccessfulRefundTotal(new BigDecimal("50.00"));
        paymentAttempt.invoice().applySuccessfulRefundTotal(new BigDecimal("50.00"));

        when(paymentAttemptRepository.findWithLockByIdAndMerchantId(paymentAttempt.id(), merchant.id()))
                .thenReturn(Optional.of(paymentAttempt));
        when(refundRepository.sumAmountByPaymentAttemptIdAndStatus(paymentAttempt.id(), RefundStatus.SUCCEEDED))
                .thenReturn(new BigDecimal("50.00"));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = refundService.createRefund(merchant.id(), UUID.randomUUID(),
                new CreateRefundRequest(paymentAttempt.id(), new BigDecimal("75.50"), "Final refund"));

        assertThat(response.status()).isEqualTo("SUCCEEDED");
        assertThat(paymentAttempt.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(paymentAttempt.invoice().status()).isEqualTo(InvoiceStatus.REFUNDED);
    }

    @Test
    void shouldRejectOverRefundWhenRefundTotalExceedsPaymentAmount() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        PaymentAttempt paymentAttempt = successfulPayment(merchant);
        UUID actorId = UUID.randomUUID();

        when(paymentAttemptRepository.findWithLockByIdAndMerchantId(paymentAttempt.id(), merchant.id()))
                .thenReturn(Optional.of(paymentAttempt));
        when(refundRepository.sumAmountByPaymentAttemptIdAndStatus(paymentAttempt.id(), RefundStatus.SUCCEEDED))
                .thenReturn(new BigDecimal("100.00"));

        assertThatThrownBy(() -> refundService.createRefund(merchant.id(), actorId,
                        new CreateRefundRequest(paymentAttempt.id(), new BigDecimal("30.00"), "Too much")))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.code()).isEqualTo("REFUND_EXCEEDS_PAYMENT_AMOUNT");
                });
        verify(auditLogService).record(merchant.id(), actorId, "REFUND_REJECTED_OVER_PAYMENT_AMOUNT",
                "PAYMENT_ATTEMPT", paymentAttempt.id(), "100.00", "130.00");
    }

    @Test
    void shouldRejectRefundForFailedPayment() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        PaymentAttempt paymentAttempt = failedPayment(merchant);

        when(paymentAttemptRepository.findWithLockByIdAndMerchantId(paymentAttempt.id(), merchant.id()))
                .thenReturn(Optional.of(paymentAttempt));

        assertThatThrownBy(() -> refundService.createRefund(merchant.id(), UUID.randomUUID(),
                        new CreateRefundRequest(paymentAttempt.id(), new BigDecimal("50.00"), "Not refundable")))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.code()).isEqualTo("PAYMENT_NOT_REFUNDABLE");
                });
    }

    private PaymentAttempt successfulPayment(Merchant merchant) {
        PaymentAttempt paymentAttempt = new PaymentAttempt(merchant, issuedInvoice(merchant), "SIM-PAY-123", PaymentMethod.CARD_SIMULATED);
        paymentAttempt.transitionTo(PaymentStatus.PROCESSING, null);
        paymentAttempt.transitionTo(PaymentStatus.SUCCEEDED, null);
        paymentAttempt.invoice().markPaid();
        return paymentAttempt;
    }

    private PaymentAttempt failedPayment(Merchant merchant) {
        PaymentAttempt paymentAttempt = new PaymentAttempt(merchant, issuedInvoice(merchant), "SIM-PAY-456", PaymentMethod.CARD_SIMULATED);
        paymentAttempt.transitionTo(PaymentStatus.PROCESSING, null);
        paymentAttempt.transitionTo(PaymentStatus.FAILED, "Insufficient funds");
        return paymentAttempt;
    }

    private Invoice issuedInvoice(Merchant merchant) {
        Customer customer = new Customer(merchant, "Lerato Khumalo", "lerato@example.com", "+27821234567");
        return new Invoice(merchant, customer, "INV-TEST123", "Grocery order", new BigDecimal("125.50"), null);
    }
}
