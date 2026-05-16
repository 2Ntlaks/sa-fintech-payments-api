package za.co.safintech.payments.payment.domain;

public enum PaymentStatus {
    CREATED,
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    EXPIRED
}
