package za.co.safintech.payments.settlement.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import za.co.safintech.payments.merchant.domain.Merchant;
import za.co.safintech.payments.payment.domain.PaymentAttempt;

@Entity
@Table(name = "settlement_batch_items")
public class SettlementBatchItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_batch_id", nullable = false)
    private SettlementBatch settlementBatch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_attempt_id", nullable = false)
    private PaymentAttempt paymentAttempt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Column(nullable = false)
    private Instant createdAt;

    protected SettlementBatchItem() {
    }

    public SettlementBatchItem(SettlementBatch settlementBatch, PaymentAttempt paymentAttempt, BigDecimal refundAmount) {
        this.id = UUID.randomUUID();
        this.settlementBatch = settlementBatch;
        this.merchant = paymentAttempt.merchant();
        this.paymentAttempt = paymentAttempt;
        this.grossAmount = paymentAttempt.grossAmount().setScale(2, RoundingMode.UNNECESSARY);
        this.feeAmount = paymentAttempt.feeAmount().setScale(2, RoundingMode.UNNECESSARY);
        this.refundAmount = refundAmount.setScale(2, RoundingMode.UNNECESSARY);
        this.netAmount = grossAmount.subtract(feeAmount).subtract(this.refundAmount).setScale(2, RoundingMode.UNNECESSARY);
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public SettlementBatch settlementBatch() {
        return settlementBatch;
    }

    public Merchant merchant() {
        return merchant;
    }

    public PaymentAttempt paymentAttempt() {
        return paymentAttempt;
    }

    public BigDecimal grossAmount() {
        return grossAmount;
    }

    public BigDecimal feeAmount() {
        return feeAmount;
    }

    public BigDecimal refundAmount() {
        return refundAmount;
    }

    public BigDecimal netAmount() {
        return netAmount;
    }
}
