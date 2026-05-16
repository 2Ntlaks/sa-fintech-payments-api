package za.co.safintech.payments.reconciliation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.safintech.payments.audit.service.AuditLogService;
import za.co.safintech.payments.common.exception.ApiException;
import za.co.safintech.payments.payment.domain.PaymentAttempt;
import za.co.safintech.payments.payment.repository.PaymentAttemptRepository;
import za.co.safintech.payments.reconciliation.domain.ReconciliationReport;
import za.co.safintech.payments.reconciliation.domain.ReconciliationReportItem;
import za.co.safintech.payments.reconciliation.domain.ReconciliationResultType;
import za.co.safintech.payments.reconciliation.dto.CreateReconciliationReportRequest;
import za.co.safintech.payments.reconciliation.dto.MockProviderPaymentRecord;
import za.co.safintech.payments.reconciliation.dto.ReconciliationReportItemResponse;
import za.co.safintech.payments.reconciliation.dto.ReconciliationReportResponse;
import za.co.safintech.payments.reconciliation.repository.ReconciliationReportRepository;

@Service
@Profile("!local")
public class ReconciliationService {

    private final ReconciliationReportRepository reconciliationReportRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final AuditLogService auditLogService;

    public ReconciliationService(ReconciliationReportRepository reconciliationReportRepository,
            PaymentAttemptRepository paymentAttemptRepository, AuditLogService auditLogService) {
        this.reconciliationReportRepository = reconciliationReportRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ReconciliationReportResponse createReport(UUID merchantId, UUID actorId,
            CreateReconciliationReportRequest request) {
        List<PaymentAttempt> internalPayments = paymentAttemptRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchantId);
        if (internalPayments.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "NO_INTERNAL_PAYMENTS",
                    "No internal payments are available for reconciliation");
        }

        Map<String, PaymentAttempt> internalByReference = internalPayments.stream()
                .collect(Collectors.toMap(PaymentAttempt::providerReference, Function.identity()));
        ReconciliationReport report = new ReconciliationReport(internalPayments.get(0).merchant());
        Set<String> seenExternalReferences = new HashSet<>();
        Set<String> matchedExternalReferences = new HashSet<>();
        Map<String, Integer> externalReferenceCounts = countExternalReferences(request.records());

        for (MockProviderPaymentRecord externalRecord : request.records()) {
            String providerReference = externalRecord.providerReference();
            if (externalReferenceCounts.get(providerReference) > 1 && !seenExternalReferences.add(providerReference)) {
                addExternalOnlyItem(report, providerReference, externalRecord, ReconciliationResultType.DUPLICATE_EXTERNAL_REFERENCE,
                        "Provider report contains the same provider reference more than once");
                continue;
            }
            seenExternalReferences.add(providerReference);

            PaymentAttempt internalPayment = internalByReference.get(providerReference);
            if (internalPayment == null) {
                addExternalOnlyItem(report, providerReference, externalRecord, ReconciliationResultType.MISSING_INTERNAL,
                        "Provider record has no matching internal payment");
                continue;
            }

            matchedExternalReferences.add(providerReference);
            report.addItem(compareRecord(report, internalPayment, externalRecord));
        }

        for (PaymentAttempt internalPayment : internalPayments) {
            if (!matchedExternalReferences.contains(internalPayment.providerReference())) {
                report.addItem(new ReconciliationReportItem(
                        report,
                        internalPayment.merchant(),
                        internalPayment.providerReference(),
                        internalPayment,
                        ReconciliationResultType.MISSING_EXTERNAL,
                        internalPayment.amount(),
                        null,
                        internalPayment.currency(),
                        null,
                        internalPayment.status().name(),
                        null,
                        "Internal payment is missing from provider report"));
            }
        }

        ReconciliationReport savedReport = reconciliationReportRepository.save(report);
        auditExceptions(merchantId, actorId, savedReport);
        return toResponse(savedReport);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationReportResponse> listReports(UUID merchantId) {
        return reconciliationReportRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReconciliationReportResponse getReport(UUID merchantId, UUID reportId) {
        return reconciliationReportRepository.findByIdAndMerchantId(reportId, merchantId)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RECONCILIATION_REPORT_NOT_FOUND",
                        "Reconciliation report not found for this merchant"));
    }

    private ReconciliationReportItem compareRecord(ReconciliationReport report, PaymentAttempt internalPayment,
            MockProviderPaymentRecord externalRecord) {
        BigDecimal externalAmount = normalizeAmount(externalRecord.amount());
        String externalCurrency = externalRecord.currency().toUpperCase(Locale.ROOT);
        String externalStatus = externalRecord.status().toUpperCase(Locale.ROOT);

        ReconciliationResultType resultType = ReconciliationResultType.MATCHED;
        String details = "Internal payment matches provider record";
        if (internalPayment.amount().compareTo(externalAmount) != 0 || !internalPayment.currency().equals(externalCurrency)) {
            resultType = ReconciliationResultType.AMOUNT_MISMATCH;
            details = "Internal amount or currency differs from provider record";
        } else if (!internalPayment.status().name().equals(externalStatus)) {
            resultType = ReconciliationResultType.STATUS_MISMATCH;
            details = "Internal payment status differs from provider record";
        }

        return new ReconciliationReportItem(
                report,
                internalPayment.merchant(),
                internalPayment.providerReference(),
                internalPayment,
                resultType,
                internalPayment.amount(),
                externalAmount,
                internalPayment.currency(),
                externalCurrency,
                internalPayment.status().name(),
                externalStatus,
                details);
    }

    private void addExternalOnlyItem(ReconciliationReport report, String providerReference,
            MockProviderPaymentRecord externalRecord, ReconciliationResultType resultType, String details) {
        report.addItem(new ReconciliationReportItem(
                report,
                report.merchant(),
                providerReference,
                null,
                resultType,
                null,
                normalizeAmount(externalRecord.amount()),
                null,
                externalRecord.currency().toUpperCase(Locale.ROOT),
                null,
                externalRecord.status().toUpperCase(Locale.ROOT),
                details));
    }

    private void auditExceptions(UUID merchantId, UUID actorId, ReconciliationReport report) {
        for (ReconciliationReportItem item : report.items()) {
            if (item.resultType() != ReconciliationResultType.MATCHED) {
                auditLogService.record(merchantId, actorId, "RECONCILIATION_EXCEPTION_DETECTED",
                        "RECONCILIATION_REPORT", report.id(), null, item.resultType().name());
            }
        }
    }

    private Map<String, Integer> countExternalReferences(List<MockProviderPaymentRecord> records) {
        Map<String, Integer> counts = new HashMap<>();
        for (MockProviderPaymentRecord record : records) {
            counts.merge(record.providerReference(), 1, Integer::sum);
        }
        return counts;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private ReconciliationReportResponse toResponse(ReconciliationReport report) {
        return new ReconciliationReportResponse(
                report.id(),
                report.merchant().id(),
                report.status().name(),
                report.totalRecords(),
                report.matchedCount(),
                report.exceptionCount(),
                report.items().stream()
                        .map(this::toItemResponse)
                        .toList());
    }

    private ReconciliationReportItemResponse toItemResponse(ReconciliationReportItem item) {
        UUID internalPaymentId = item.internalPaymentAttempt() == null ? null : item.internalPaymentAttempt().id();
        return new ReconciliationReportItemResponse(
                item.id(),
                item.providerReference(),
                internalPaymentId,
                item.resultType().name(),
                item.internalAmount(),
                item.externalAmount(),
                item.internalCurrency(),
                item.externalCurrency(),
                item.internalStatus(),
                item.externalStatus(),
                item.details());
    }
}
