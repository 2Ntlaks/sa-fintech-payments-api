package za.co.safintech.payments.invoice.domain;

public enum InvoiceStatus {
    ISSUED,
    PAID,
    EXPIRED,
    CANCELLED,
    PARTIALLY_REFUNDED,
    REFUNDED
}
