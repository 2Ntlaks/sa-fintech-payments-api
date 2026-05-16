package za.co.safintech.payments.reconciliation.dto;

import java.util.List;
import java.util.UUID;

public record ReconciliationReportResponse(
        UUID id,
        UUID merchantId,
        String status,
        int totalRecords,
        int matchedCount,
        int exceptionCount,
        List<ReconciliationReportItemResponse> items) {
}
