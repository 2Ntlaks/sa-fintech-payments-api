package za.co.safintech.payments.refund.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.safintech.payments.audit.service.AuditLogService;
import za.co.safintech.payments.balance.service.MerchantBalanceService;
import za.co.safintech.payments.common.exception.ApiException;
import za.co.safintech.payments.payment.domain.PaymentAttempt;
import za.co.safintech.payments.payment.domain.PaymentStatus;
import za.co.safintech.payments.payment.repository.PaymentAttemptRepository;
import za.co.safintech.payments.refund.domain.Refund;
import za.co.safintech.payments.refund.domain.RefundStatus;
import za.co.safintech.payments.refund.dto.CreateRefundRequest;
import za.co.safintech.payments.refund.dto.RefundResponse;
import za.co.safintech.payments.refund.repository.RefundRepository;

@Service
@Profile("!local")
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final AuditLogService auditLogService;
    private final MerchantBalanceService merchantBalanceService;

    public RefundService(RefundRepository refundRepository, PaymentAttemptRepository paymentAttemptRepository,
            AuditLogService auditLogService, MerchantBalanceService merchantBalanceService) {
        this.refundRepository = refundRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.auditLogService = auditLogService;
        this.merchantBalanceService = merchantBalanceService;
    }

    @Transactional
    public RefundResponse createRefund(UUID merchantId, UUID actorId, CreateRefundRequest request) {
        PaymentAttempt paymentAttempt = paymentAttemptRepository.findWithLockByIdAndMerchantId(request.paymentId(), merchantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found for this merchant"));

        if (paymentAttempt.status() != PaymentStatus.SUCCEEDED
                && paymentAttempt.status() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new ApiException(HttpStatus.CONFLICT, "PAYMENT_NOT_REFUNDABLE",
                    "Only successful payments with remaining refundable amount can be refunded");
        }

        BigDecimal refundAmount = normalizeAmount(request.amount());
        BigDecimal alreadyRefunded = refundRepository.sumAmountByPaymentAttemptIdAndStatus(paymentAttempt.id(), RefundStatus.SUCCEEDED)
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal newRefundedTotal = alreadyRefunded.add(refundAmount).setScale(2, RoundingMode.UNNECESSARY);

        if (newRefundedTotal.compareTo(paymentAttempt.amount()) > 0) {
            auditLogService.record(merchantId, actorId, "REFUND_REJECTED_OVER_PAYMENT_AMOUNT",
                    "PAYMENT_ATTEMPT", paymentAttempt.id(), alreadyRefunded.toPlainString(), newRefundedTotal.toPlainString());
            throw new ApiException(HttpStatus.CONFLICT, "REFUND_EXCEEDS_PAYMENT_AMOUNT",
                    "Refund total cannot exceed the original successful payment amount");
        }

        PaymentStatus previousPaymentStatus = paymentAttempt.status();
        String previousInvoiceStatus = paymentAttempt.invoice().status().name();
        Refund refund = refundRepository.save(new Refund(
                paymentAttempt,
                refundAmount,
                nextProviderReference(),
                request.reason()));
        refund.transitionTo(RefundStatus.SUCCEEDED);
        paymentAttempt.applySuccessfulRefundTotal(newRefundedTotal);
        paymentAttempt.invoice().applySuccessfulRefundTotal(newRefundedTotal);
        merchantBalanceService.applySuccessfulRefund(merchantId, refundAmount);

        auditLogService.record(merchantId, actorId, "REFUND_SUCCEEDED", "REFUND",
                refund.id(), RefundStatus.REQUESTED.name(), RefundStatus.SUCCEEDED.name());
        auditLogService.record(merchantId, actorId, "PAYMENT_REFUND_STATE_CHANGED", "PAYMENT_ATTEMPT",
                paymentAttempt.id(), previousPaymentStatus.name(), paymentAttempt.status().name());
        auditLogService.record(merchantId, actorId, "INVOICE_REFUND_STATE_CHANGED", "INVOICE",
                paymentAttempt.invoice().id(), previousInvoiceStatus, paymentAttempt.invoice().status().name());

        return toResponse(refund);
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> listRefunds(UUID merchantId) {
        return refundRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RefundResponse getRefund(UUID merchantId, UUID refundId) {
        return refundRepository.findByIdAndMerchantId(refundId, merchantId)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REFUND_NOT_FOUND", "Refund not found for this merchant"));
    }

    private RefundResponse toResponse(Refund refund) {
        return new RefundResponse(
                refund.id(),
                refund.merchant().id(),
                refund.paymentAttempt().id(),
                refund.amount(),
                refund.currency(),
                refund.status().name(),
                refund.providerReference(),
                refund.reason());
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REFUND_AMOUNT_SCALE",
                    "Refund amount must have at most two decimal places");
        }
    }

    private String nextProviderReference() {
        return "SIM-REF-" + UUID.randomUUID();
    }
}
