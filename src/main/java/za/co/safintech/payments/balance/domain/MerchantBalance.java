package za.co.safintech.payments.balance.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "merchant_balances")
public class MerchantBalance {

    @Id
    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal availableAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal settledAmount;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected MerchantBalance() {
    }

    public MerchantBalance(UUID merchantId) {
        this.merchantId = merchantId;
        this.currency = "ZAR";
        this.grossAmount = money("0.00");
        this.feeAmount = money("0.00");
        this.refundedAmount = money("0.00");
        this.availableAmount = money("0.00");
        this.settledAmount = money("0.00");
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void applySuccessfulPayment(BigDecimal grossAmount, BigDecimal feeAmount, BigDecimal netAmount) {
        this.grossAmount = this.grossAmount.add(normalize(grossAmount));
        this.feeAmount = this.feeAmount.add(normalize(feeAmount));
        this.availableAmount = this.availableAmount.add(normalize(netAmount));
    }

    public void applySuccessfulRefund(BigDecimal refundAmount) {
        BigDecimal normalizedRefund = normalize(refundAmount);
        this.refundedAmount = this.refundedAmount.add(normalizedRefund);
        this.availableAmount = this.availableAmount.subtract(normalizedRefund);
    }

    public void applySettlement(BigDecimal settlementAmount) {
        BigDecimal normalizedSettlementAmount = normalize(settlementAmount);
        this.availableAmount = this.availableAmount.subtract(normalizedSettlementAmount);
        this.settledAmount = this.settledAmount.add(normalizedSettlementAmount);
    }

    public UUID merchantId() {
        return merchantId;
    }

    public String currency() {
        return currency;
    }

    public BigDecimal grossAmount() {
        return grossAmount;
    }

    public BigDecimal feeAmount() {
        return feeAmount;
    }

    public BigDecimal refundedAmount() {
        return refundedAmount;
    }

    public BigDecimal availableAmount() {
        return availableAmount;
    }

    public BigDecimal settledAmount() {
        return settledAmount;
    }

    private BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private BigDecimal money(String amount) {
        return new BigDecimal(amount).setScale(2, RoundingMode.UNNECESSARY);
    }
}
