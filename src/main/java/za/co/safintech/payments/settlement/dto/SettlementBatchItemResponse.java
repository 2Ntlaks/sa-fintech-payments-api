package za.co.safintech.payments.settlement.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementBatchItemResponse(
        UUID id,
        UUID paymentId,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal refundAmount,
        BigDecimal netAmount) {
}
