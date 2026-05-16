package za.co.safintech.payments.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import za.co.safintech.payments.refund.domain.RefundStatus;
import za.co.safintech.payments.refund.repository.RefundRepository;
import za.co.safintech.payments.settlement.domain.SettlementBatch;
import za.co.safintech.payments.settlement.repository.SettlementBatchRepository;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementBatchRepository settlementBatchRepository;

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private MerchantBalanceService merchantBalanceService;

    @Mock
    private AuditLogService auditLogService;

    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(
                settlementBatchRepository,
                paymentAttemptRepository,
                refundRepository,
                merchantBalanceService,
                auditLogService);
    }

    @Test
    void shouldCreateSettlementBatchForEligiblePaymentsAndPreserveTotals() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        PaymentAttempt paidPayment = successfulPayment(merchant, "SIM-PAY-1");
        PaymentAttempt partiallyRefundedPayment = successfulPayment(merchant, "SIM-PAY-2");
        partiallyRefundedPayment.applySuccessfulRefundTotal(new BigDecimal("50.00"));
        UUID actorId = UUID.randomUUID();

        when(paymentAttemptRepository.findEligibleForSettlementWithLock(
                merchant.id(), List.of(PaymentStatus.SUCCEEDED, PaymentStatus.PARTIALLY_REFUNDED)))
                .thenReturn(List.of(paidPayment, partiallyRefundedPayment));
        when(refundRepository.sumAmountByPaymentAttemptIdAndStatus(paidPayment.id(), RefundStatus.SUCCEEDED))
                .thenReturn(new BigDecimal("0.00"));
        when(refundRepository.sumAmountByPaymentAttemptIdAndStatus(partiallyRefundedPayment.id(), RefundStatus.SUCCEEDED))
                .thenReturn(new BigDecimal("50.00"));
        when(settlementBatchRepository.save(any(SettlementBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = settlementService.createSettlementBatch(merchant.id(), actorId);

        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.items()).hasSize(2);
        assertThat(response.grossAmount()).isEqualByComparingTo("251.00");
        assertThat(response.feeAmount()).isEqualByComparingTo("9.28");
        assertThat(response.refundAmount()).isEqualByComparingTo("50.00");
        assertThat(response.netAmount()).isEqualByComparingTo("191.72");
        verify(merchantBalanceService).applySettlement(merchant.id(), new BigDecimal("191.72"));
        verify(auditLogService).record(merchant.id(), actorId, "SETTLEMENT_BATCH_CREATED", "SETTLEMENT_BATCH",
                response.id(), null, "CREATED");
    }

    @Test
    void shouldRejectSettlementBatchWhenNoPaymentsAreEligible() {
        UUID merchantId = UUID.randomUUID();

        when(paymentAttemptRepository.findEligibleForSettlementWithLock(
                merchantId, List.of(PaymentStatus.SUCCEEDED, PaymentStatus.PARTIALLY_REFUNDED)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> settlementService.createSettlementBatch(merchantId, UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.code()).isEqualTo("NO_SETTLEMENT_ELIGIBLE_PAYMENTS");
                });
    }

    @Test
    void shouldReturnMerchantScopedSettlementBatch() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        SettlementBatch batch = new SettlementBatch(merchant);

        when(settlementBatchRepository.findByIdAndMerchantId(batch.id(), merchant.id()))
                .thenReturn(Optional.of(batch));

        var response = settlementService.getSettlementBatch(merchant.id(), batch.id());

        assertThat(response.id()).isEqualTo(batch.id());
        assertThat(response.merchantId()).isEqualTo(merchant.id());
    }

    private PaymentAttempt successfulPayment(Merchant merchant, String providerReference) {
        PaymentAttempt paymentAttempt = new PaymentAttempt(merchant, issuedInvoice(merchant), providerReference, PaymentMethod.CARD_SIMULATED);
        paymentAttempt.transitionTo(PaymentStatus.PROCESSING, null);
        paymentAttempt.applySuccessfulFee(new BigDecimal("4.64"));
        paymentAttempt.transitionTo(PaymentStatus.SUCCEEDED, null);
        paymentAttempt.invoice().markPaid();
        return paymentAttempt;
    }

    private Invoice issuedInvoice(Merchant merchant) {
        Customer customer = new Customer(merchant, "Lerato Khumalo", "lerato@example.com", "+27821234567");
        return new Invoice(merchant, customer, "INV-" + UUID.randomUUID(), "Grocery order", new BigDecimal("125.50"), null);
    }
}
