package za.co.safintech.payments.payment.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.safintech.payments.audit.service.AuditLogService;
import za.co.safintech.payments.common.exception.ApiException;
import za.co.safintech.payments.invoice.domain.Invoice;
import za.co.safintech.payments.invoice.domain.InvoiceStatus;
import za.co.safintech.payments.invoice.repository.InvoiceRepository;
import za.co.safintech.payments.payment.domain.PaymentAttempt;
import za.co.safintech.payments.payment.domain.PaymentMethod;
import za.co.safintech.payments.payment.domain.PaymentStatus;
import za.co.safintech.payments.payment.dto.CreatePaymentRequest;
import za.co.safintech.payments.payment.dto.PaymentResponse;
import za.co.safintech.payments.payment.dto.UpdatePaymentStatusRequest;
import za.co.safintech.payments.payment.repository.PaymentAttemptRepository;

@Service
@Profile("!local")
public class PaymentService {

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditLogService auditLogService;

    public PaymentService(PaymentAttemptRepository paymentAttemptRepository, InvoiceRepository invoiceRepository,
            AuditLogService auditLogService) {
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.invoiceRepository = invoiceRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PaymentResponse createPaymentAttempt(UUID merchantId, UUID actorId, CreatePaymentRequest request) {
        Invoice invoice = invoiceRepository.findByIdAndMerchantId(request.invoiceId(), merchantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found for this merchant"));

        if (invoice.status() != InvoiceStatus.ISSUED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVOICE_NOT_PAYABLE", "Only issued invoices can receive payment attempts");
        }

        PaymentMethod paymentMethod = parsePaymentMethod(request.paymentMethod());
        PaymentAttempt paymentAttempt = paymentAttemptRepository.save(new PaymentAttempt(
                invoice.merchant(),
                invoice,
                nextProviderReference(),
                paymentMethod));

        auditLogService.record(merchantId, actorId, "PAYMENT_ATTEMPT_CREATED", "PAYMENT_ATTEMPT",
                paymentAttempt.id(), null, paymentAttempt.status().name());

        return toResponse(paymentAttempt);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments(UUID merchantId) {
        return paymentAttemptRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID merchantId, UUID paymentId) {
        return paymentAttemptRepository.findByIdAndMerchantId(paymentId, merchantId)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found for this merchant"));
    }

    @Transactional
    public PaymentResponse updatePaymentStatus(UUID merchantId, UUID actorId, UUID paymentId, UpdatePaymentStatusRequest request) {
        PaymentAttempt paymentAttempt = paymentAttemptRepository.findWithLockByIdAndMerchantId(paymentId, merchantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found for this merchant"));

        PaymentStatus previousStatus = paymentAttempt.status();
        PaymentStatus nextStatus = parsePaymentStatus(request.status());

        if (nextStatus == PaymentStatus.SUCCEEDED
                && paymentAttemptRepository.existsByInvoiceIdAndStatus(paymentAttempt.invoice().id(), PaymentStatus.SUCCEEDED)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVOICE_ALREADY_PAID", "Invoice already has a successful payment");
        }

        try {
            paymentAttempt.transitionTo(nextStatus, request.failureReason());
            if (nextStatus == PaymentStatus.SUCCEEDED) {
                paymentAttempt.invoice().markPaid();
            }
        } catch (IllegalStateException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_PAYMENT_STATUS_TRANSITION", exception.getMessage());
        }

        auditLogService.record(merchantId, actorId, "PAYMENT_STATUS_CHANGED", "PAYMENT_ATTEMPT",
                paymentAttempt.id(), previousStatus.name(), nextStatus.name());

        return toResponse(paymentAttempt);
    }

    private PaymentResponse toResponse(PaymentAttempt paymentAttempt) {
        return new PaymentResponse(
                paymentAttempt.id(),
                paymentAttempt.merchant().id(),
                paymentAttempt.invoice().id(),
                paymentAttempt.amount(),
                paymentAttempt.currency(),
                paymentAttempt.paymentMethod().name(),
                paymentAttempt.status().name(),
                paymentAttempt.providerReference(),
                paymentAttempt.failureReason());
    }

    private PaymentMethod parsePaymentMethod(String paymentMethod) {
        try {
            return PaymentMethod.valueOf(paymentMethod.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PAYMENT_METHOD", "Unsupported simulated payment method");
        }
    }

    private PaymentStatus parsePaymentStatus(String status) {
        try {
            return PaymentStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PAYMENT_STATUS", "Unsupported payment status");
        }
    }

    private String nextProviderReference() {
        return "SIM-PAY-" + UUID.randomUUID();
    }
}
