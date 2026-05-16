package za.co.safintech.payments.reconciliation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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
import za.co.safintech.payments.common.exception.ApiException;
import za.co.safintech.payments.customer.domain.Customer;
import za.co.safintech.payments.invoice.domain.Invoice;
import za.co.safintech.payments.merchant.domain.Merchant;
import za.co.safintech.payments.merchant.domain.MerchantType;
import za.co.safintech.payments.payment.domain.PaymentAttempt;
import za.co.safintech.payments.payment.domain.PaymentMethod;
import za.co.safintech.payments.payment.domain.PaymentStatus;
import za.co.safintech.payments.payment.repository.PaymentAttemptRepository;
import za.co.safintech.payments.reconciliation.domain.ReconciliationReport;
import za.co.safintech.payments.reconciliation.dto.CreateReconciliationReportRequest;
import za.co.safintech.payments.reconciliation.dto.MockProviderPaymentRecord;
import za.co.safintech.payments.reconciliation.repository.ReconciliationReportRepository;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private ReconciliationReportRepository reconciliationReportRepository;

    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    private AuditLogService auditLogService;

    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        reconciliationService = new ReconciliationService(
                reconciliationReportRepository,
                paymentAttemptRepository,
                auditLogService);
    }

    @Test
    void shouldCreateReportWithMatchedAndExceptionItemsWithoutMutatingPayments() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        PaymentAttempt matchedPayment = payment(merchant, "SIM-PAY-MATCH", "125.50", PaymentStatus.SUCCEEDED);
        PaymentAttempt amountMismatchPayment = payment(merchant, "SIM-PAY-AMOUNT", "80.00", PaymentStatus.SUCCEEDED);
        PaymentAttempt statusMismatchPayment = payment(merchant, "SIM-PAY-STATUS", "40.00", PaymentStatus.SUCCEEDED);
        PaymentAttempt missingExternalPayment = payment(merchant, "SIM-PAY-MISSING-EXTERNAL", "20.00", PaymentStatus.SUCCEEDED);
        UUID actorId = UUID.randomUUID();
        CreateReconciliationReportRequest request = new CreateReconciliationReportRequest(List.of(
                providerRecord("SIM-PAY-MATCH", "125.50", "SUCCEEDED"),
                providerRecord("SIM-PAY-AMOUNT", "70.00", "SUCCEEDED"),
                providerRecord("SIM-PAY-STATUS", "40.00", "FAILED"),
                providerRecord("SIM-PAY-MISSING-INTERNAL", "10.00", "SUCCEEDED"),
                providerRecord("SIM-PAY-MATCH", "125.50", "SUCCEEDED")));

        when(paymentAttemptRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchant.id()))
                .thenReturn(List.of(matchedPayment, amountMismatchPayment, statusMismatchPayment, missingExternalPayment));
        when(reconciliationReportRepository.save(any(ReconciliationReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = reconciliationService.createReport(merchant.id(), actorId, request);

        assertThat(response.totalRecords()).isEqualTo(6);
        assertThat(response.matchedCount()).isEqualTo(1);
        assertThat(response.exceptionCount()).isEqualTo(5);
        assertThat(response.items())
                .extracting("resultType")
                .containsExactlyInAnyOrder(
                        "MATCHED",
                        "AMOUNT_MISMATCH",
                        "STATUS_MISMATCH",
                        "MISSING_INTERNAL",
                        "DUPLICATE_EXTERNAL_REFERENCE",
                        "MISSING_EXTERNAL");
        assertThat(amountMismatchPayment.amount()).isEqualByComparingTo("80.00");
        assertThat(statusMismatchPayment.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(auditLogService, times(5)).record(
                org.mockito.ArgumentMatchers.eq(merchant.id()),
                org.mockito.ArgumentMatchers.eq(actorId),
                org.mockito.ArgumentMatchers.eq("RECONCILIATION_EXCEPTION_DETECTED"),
                org.mockito.ArgumentMatchers.eq("RECONCILIATION_REPORT"),
                org.mockito.ArgumentMatchers.eq(response.id()),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldRejectReportWhenMerchantHasNoInternalPayments() {
        UUID merchantId = UUID.randomUUID();

        when(paymentAttemptRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchantId)).thenReturn(List.of());

        assertThatThrownBy(() -> reconciliationService.createReport(merchantId, UUID.randomUUID(),
                        new CreateReconciliationReportRequest(List.of(providerRecord("SIM-PAY-1", "10.00", "SUCCEEDED")))))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.code()).isEqualTo("NO_INTERNAL_PAYMENTS");
                });
    }

    @Test
    void shouldReturnMerchantScopedReport() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        ReconciliationReport report = new ReconciliationReport(merchant);

        when(reconciliationReportRepository.findByIdAndMerchantId(report.id(), merchant.id()))
                .thenReturn(Optional.of(report));

        var response = reconciliationService.getReport(merchant.id(), report.id());

        assertThat(response.id()).isEqualTo(report.id());
        assertThat(response.merchantId()).isEqualTo(merchant.id());
    }

    private MockProviderPaymentRecord providerRecord(String providerReference, String amount, String status) {
        return new MockProviderPaymentRecord(providerReference, new BigDecimal(amount), "ZAR", status);
    }

    private PaymentAttempt payment(Merchant merchant, String providerReference, String amount, PaymentStatus status) {
        PaymentAttempt paymentAttempt = new PaymentAttempt(
                merchant,
                invoice(merchant, amount),
                providerReference,
                PaymentMethod.CARD_SIMULATED);
        if (status == PaymentStatus.SUCCEEDED) {
            paymentAttempt.transitionTo(PaymentStatus.PROCESSING, null);
            paymentAttempt.transitionTo(PaymentStatus.SUCCEEDED, null);
            paymentAttempt.invoice().markPaid();
        }
        return paymentAttempt;
    }

    private Invoice invoice(Merchant merchant, String amount) {
        Customer customer = new Customer(merchant, "Lerato Khumalo", "lerato@example.com", "+27821234567");
        return new Invoice(merchant, customer, "INV-" + UUID.randomUUID(), "Grocery order", new BigDecimal(amount), null);
    }
}
