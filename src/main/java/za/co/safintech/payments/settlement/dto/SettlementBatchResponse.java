package za.co.safintech.payments.settlement.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SettlementBatchResponse(
        UUID id,
        UUID merchantId,
        String currency,
        String status,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal refundAmount,
        BigDecimal netAmount,
        List<SettlementBatchItemResponse> items) {
}
