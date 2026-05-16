package za.co.safintech.payments.reconciliation.domain;

public enum ReconciliationResultType {
    MATCHED,
    MISSING_INTERNAL,
    MISSING_EXTERNAL,
    AMOUNT_MISMATCH,
    STATUS_MISMATCH,
    DUPLICATE_EXTERNAL_REFERENCE
}
