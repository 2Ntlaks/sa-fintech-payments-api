package za.co.safintech.payments.payment.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.safintech.payments.audit.service.AuditLogService;
import za.co.safintech.payments.balance.service.FeeCalculator;
import za.co.safintech.payments.balance.service.MerchantBalanceService;
import za.co.safintech.payments.common.exception.ApiException;
import za.co.safintech.payments.invoice.domain.Invoice;
import za.co.safintech.payments.invoice.domain.InvoiceStatus;
import za.co.safintech.payments.invoice.repository.InvoiceRepository;
import za.co.safintech.payments.payment.domain.PaymentAttempt;
import za.co.safintech.payments.payment.domain.PaymentMethod;
import za.co.safintech.payments.payment.domain.PaymentStatus;
import za.co.safintech.payments.payment.domain.IdempotencyRecord;
import za.co.safintech.payments.payment.dto.CreatePaymentRequest;
import za.co.safintech.payments.payment.dto.PaymentResponse;
import za.co.safintech.payments.payment.dto.UpdatePaymentStatusRequest;
import za.co.safintech.payments.payment.repository.IdempotencyRecordRepository;
import za.co.safintech.payments.payment.repository.PaymentAttemptRepository;

@Service
@Profile("!local")
public class PaymentService {

    private static final String CREATE_PAYMENT_OPERATION = "CREATE_PAYMENT_ATTEMPT";
    private static final List<PaymentStatus> SUCCESSFUL_INVOICE_PAYMENT_STATUSES = List.of(
            PaymentStatus.SUCCEEDED,
            PaymentStatus.PARTIALLY_REFUNDED,
            PaymentStatus.REFUNDED);

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final InvoiceRepository invoiceRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final AuditLogService auditLogService;
    private final FeeCalculator feeCalculator;
    private final MerchantBalanceService merchantBalanceService;

    public PaymentService(PaymentAttemptRepository paymentAttemptRepository, InvoiceRepository invoiceRepository,
            IdempotencyRecordRepository idempotencyRecordRepository, AuditLogService auditLogService,
            FeeCalculator feeCalculator, MerchantBalanceService merchantBalanceService) {
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.invoiceRepository = invoiceRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.auditLogService = auditLogService;
        this.feeCalculator = feeCalculator;
        this.merchantBalanceService = merchantBalanceService;
    }

    @Transactional
    public PaymentResponse createPaymentAttempt(UUID merchantId, UUID actorId, CreatePaymentRequest request,
            String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String requestHash = requestHash(request);

        if (normalizedKey != null) {
            var existingRecord = idempotencyRecordRepository.findByMerchantIdAndOperationAndIdempotencyKey(
                    merchantId, CREATE_PAYMENT_OPERATION, normalizedKey);
            if (existingRecord.isPresent()) {
                IdempotencyRecord record = existingRecord.get();
                if (!record.requestHash().equals(requestHash)) {
                    auditLogService.record(merchantId, actorId, "IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST",
                            "IDEMPOTENCY_RECORD", record.targetId(), null, CREATE_PAYMENT_OPERATION);
                    throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                            "Idempotency key was reused with a different payment request");
                }
                return getPayment(merchantId, record.targetId());
            }
        }

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

        if (normalizedKey != null) {
            idempotencyRecordRepository.save(new IdempotencyRecord(
                    merchantId,
                    CREATE_PAYMENT_OPERATION,
                    normalizedKey,
                    requestHash,
                    "PAYMENT_ATTEMPT",
                    paymentAttempt.id()));
        }

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
                && paymentAttemptRepository.existsByInvoiceIdAndStatusIn(
                        paymentAttempt.invoice().id(), SUCCESSFUL_INVOICE_PAYMENT_STATUSES)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVOICE_ALREADY_PAID", "Invoice already has a successful payment");
        }

        try {
            paymentAttempt.transitionTo(nextStatus, request.failureReason());
            if (nextStatus == PaymentStatus.SUCCEEDED) {
                paymentAttempt.applySuccessfulFee(feeCalculator.calculateFee(paymentAttempt.grossAmount()));
                paymentAttempt.invoice().markPaid();
                merchantBalanceService.applySuccessfulPayment(
                        merchantId,
                        paymentAttempt.grossAmount(),
                        paymentAttempt.feeAmount(),
                        paymentAttempt.netAmount());
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
                paymentAttempt.grossAmount(),
                paymentAttempt.feeAmount(),
                paymentAttempt.netAmount(),
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

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String normalizedKey = idempotencyKey.trim();
        if (normalizedKey.length() > 120) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_TOO_LONG",
                    "Idempotency key must be 120 characters or fewer");
        }
        return normalizedKey;
    }

    private String requestHash(CreatePaymentRequest request) {
        String fingerprint = request.invoiceId() + "|" + request.paymentMethod().toUpperCase(Locale.ROOT);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fingerprint.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is required", exception);
        }
    }
}
