package za.co.safintech.payments.reconciliation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ReconciliationReportItemResponse(
        UUID id,
        String providerReference,
        UUID internalPaymentId,
        String resultType,
        BigDecimal internalAmount,
        BigDecimal externalAmount,
        String internalCurrency,
        String externalCurrency,
        String internalStatus,
        String externalStatus,
        String details) {
}
