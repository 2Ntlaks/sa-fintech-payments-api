package za.co.safintech.payments.refund.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import za.co.safintech.payments.merchant.domain.Merchant;
import za.co.safintech.payments.payment.domain.PaymentAttempt;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_attempt_id", nullable = false)
    private PaymentAttempt paymentAttempt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RefundStatus status;

    @Column(nullable = false, length = 64)
    private String providerReference;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false)
    private Instant statusChangedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Refund() {
    }

    public Refund(PaymentAttempt paymentAttempt, BigDecimal amount, String providerReference, String reason) {
        this.id = UUID.randomUUID();
        this.merchant = paymentAttempt.merchant();
        this.paymentAttempt = paymentAttempt;
        this.amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        this.currency = paymentAttempt.currency();
        this.status = RefundStatus.REQUESTED;
        this.providerReference = providerReference;
        this.reason = reason;
        this.statusChangedAt = Instant.now();
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

    public void transitionTo(RefundStatus nextStatus) {
        if (!RefundStatusTransitions.canMove(status, nextStatus)) {
            throw new IllegalStateException("Refund cannot move from " + status + " to " + nextStatus);
        }
        status = nextStatus;
        statusChangedAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public Merchant merchant() {
        return merchant;
    }

    public PaymentAttempt paymentAttempt() {
        return paymentAttempt;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public RefundStatus status() {
        return status;
    }

    public String providerReference() {
        return providerReference;
    }

    public String reason() {
        return reason;
    }
}
