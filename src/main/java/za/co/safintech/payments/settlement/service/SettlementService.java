package za.co.safintech.payments.settlement.service;

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
import za.co.safintech.payments.refund.domain.RefundStatus;
import za.co.safintech.payments.refund.repository.RefundRepository;
import za.co.safintech.payments.settlement.domain.SettlementBatch;
import za.co.safintech.payments.settlement.domain.SettlementBatchItem;
import za.co.safintech.payments.settlement.dto.SettlementBatchItemResponse;
import za.co.safintech.payments.settlement.dto.SettlementBatchResponse;
import za.co.safintech.payments.settlement.repository.SettlementBatchRepository;

@Service
@Profile("!local")
public class SettlementService {

    private static final List<PaymentStatus> SETTLEMENT_ELIGIBLE_STATUSES = List.of(
            PaymentStatus.SUCCEEDED,
            PaymentStatus.PARTIALLY_REFUNDED);

    private final SettlementBatchRepository settlementBatchRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final RefundRepository refundRepository;
    private final MerchantBalanceService merchantBalanceService;
    private final AuditLogService auditLogService;

    public SettlementService(SettlementBatchRepository settlementBatchRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            RefundRepository refundRepository,
            MerchantBalanceService merchantBalanceService,
            AuditLogService auditLogService) {
        this.settlementBatchRepository = settlementBatchRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.refundRepository = refundRepository;
        this.merchantBalanceService = merchantBalanceService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public SettlementBatchResponse createSettlementBatch(UUID merchantId, UUID actorId) {
        List<PaymentAttempt> eligiblePayments = paymentAttemptRepository.findEligibleForSettlementWithLock(
                merchantId, SETTLEMENT_ELIGIBLE_STATUSES);

        if (eligiblePayments.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "NO_SETTLEMENT_ELIGIBLE_PAYMENTS",
                    "No eligible payments are available for settlement");
        }

        SettlementBatch batch = new SettlementBatch(eligiblePayments.get(0).merchant());
        for (PaymentAttempt paymentAttempt : eligiblePayments) {
            BigDecimal refundAmount = refundRepository
                    .sumAmountByPaymentAttemptIdAndStatus(paymentAttempt.id(), RefundStatus.SUCCEEDED)
                    .setScale(2, RoundingMode.UNNECESSARY);
            batch.addItem(new SettlementBatchItem(batch, paymentAttempt, refundAmount));
        }

        SettlementBatch savedBatch = settlementBatchRepository.save(batch);
        merchantBalanceService.applySettlement(merchantId, savedBatch.netAmount());
        auditLogService.record(merchantId, actorId, "SETTLEMENT_BATCH_CREATED", "SETTLEMENT_BATCH",
                savedBatch.id(), null, savedBatch.status().name());

        return toResponse(savedBatch);
    }

    @Transactional(readOnly = true)
    public List<SettlementBatchResponse> listSettlementBatches(UUID merchantId) {
        return settlementBatchRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SettlementBatchResponse getSettlementBatch(UUID merchantId, UUID settlementBatchId) {
        return settlementBatchRepository.findByIdAndMerchantId(settlementBatchId, merchantId)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SETTLEMENT_BATCH_NOT_FOUND",
                        "Settlement batch not found for this merchant"));
    }

    private SettlementBatchResponse toResponse(SettlementBatch batch) {
        return new SettlementBatchResponse(
                batch.id(),
                batch.merchant().id(),
                batch.currency(),
                batch.status().name(),
                batch.grossAmount(),
                batch.feeAmount(),
                batch.refundAmount(),
                batch.netAmount(),
                batch.items().stream()
                        .map(this::toItemResponse)
                        .toList());
    }

    private SettlementBatchItemResponse toItemResponse(SettlementBatchItem item) {
        return new SettlementBatchItemResponse(
                item.id(),
                item.paymentAttempt().id(),
                item.grossAmount(),
                item.feeAmount(),
                item.refundAmount(),
                item.netAmount());
    }
}
