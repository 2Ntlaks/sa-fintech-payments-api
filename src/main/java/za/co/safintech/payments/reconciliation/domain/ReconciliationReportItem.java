package za.co.safintech.payments.reconciliation.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import za.co.safintech.payments.merchant.domain.Merchant;
import za.co.safintech.payments.payment.domain.PaymentAttempt;

@Entity
@Table(name = "reconciliation_report_items")
public class ReconciliationReportItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reconciliation_report_id", nullable = false)
    private ReconciliationReport reconciliationReport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false, length = 64)
    private String providerReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_payment_attempt_id")
    private PaymentAttempt internalPaymentAttempt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReconciliationResultType resultType;

    @Column(precision = 19, scale = 2)
    private BigDecimal internalAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal externalAmount;

    @Column(length = 3)
    private String internalCurrency;

    @Column(length = 3)
    private String externalCurrency;

    @Column(length = 40)
    private String internalStatus;

    @Column(length = 40)
    private String externalStatus;

    @Column(length = 255)
    private String details;

    @Column(nullable = false)
    private Instant createdAt;

    protected ReconciliationReportItem() {
    }

    public ReconciliationReportItem(ReconciliationReport reconciliationReport, Merchant merchant,
            String providerReference, PaymentAttempt internalPaymentAttempt, ReconciliationResultType resultType,
            BigDecimal internalAmount, BigDecimal externalAmount, String internalCurrency, String externalCurrency,
            String internalStatus, String externalStatus, String details) {
        this.id = UUID.randomUUID();
        this.reconciliationReport = reconciliationReport;
        this.merchant = merchant;
        this.providerReference = providerReference;
        this.internalPaymentAttempt = internalPaymentAttempt;
        this.resultType = resultType;
        this.internalAmount = internalAmount;
        this.externalAmount = externalAmount;
        this.internalCurrency = internalCurrency;
        this.externalCurrency = externalCurrency;
        this.internalStatus = internalStatus;
        this.externalStatus = externalStatus;
        this.details = details;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public ReconciliationReport reconciliationReport() {
        return reconciliationReport;
    }

    public Merchant merchant() {
        return merchant;
    }

    public String providerReference() {
        return providerReference;
    }

    public PaymentAttempt internalPaymentAttempt() {
        return internalPaymentAttempt;
    }

    public ReconciliationResultType resultType() {
        return resultType;
    }

    public BigDecimal internalAmount() {
        return internalAmount;
    }

    public BigDecimal externalAmount() {
        return externalAmount;
    }

    public String internalCurrency() {
        return internalCurrency;
    }

    public String externalCurrency() {
        return externalCurrency;
    }

    public String internalStatus() {
        return internalStatus;
    }

    public String externalStatus() {
        return externalStatus;
    }

    public String details() {
        return details;
    }
}
