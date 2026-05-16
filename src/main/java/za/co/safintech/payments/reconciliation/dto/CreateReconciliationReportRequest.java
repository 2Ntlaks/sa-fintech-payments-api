package za.co.safintech.payments.reconciliation.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateReconciliationReportRequest(
        @NotEmpty
        @Size(max = 500)
        List<@Valid MockProviderPaymentRecord> records) {
}
