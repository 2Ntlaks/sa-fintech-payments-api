package za.co.safintech.payments.payment.domain;

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
import za.co.safintech.payments.invoice.domain.Invoice;
import za.co.safintech.payments.merchant.domain.Merchant;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false, length = 64)
    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PaymentStatus status;

    @Column(length = 255)
    private String failureReason;

    @Column(nullable = false)
    private Instant statusChangedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected PaymentAttempt() {
    }

    public PaymentAttempt(Merchant merchant, Invoice invoice, String providerReference, PaymentMethod paymentMethod) {
        this.id = UUID.randomUUID();
        this.merchant = merchant;
        this.invoice = invoice;
        this.providerReference = providerReference;
        this.paymentMethod = paymentMethod;
        this.amount = invoice.amount().setScale(2, RoundingMode.UNNECESSARY);
        this.currency = invoice.currency();
        this.status = PaymentStatus.CREATED;
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

    public void transitionTo(PaymentStatus nextStatus, String failureReason) {
        if (!PaymentStatusTransitions.canMove(status, nextStatus)) {
            throw new IllegalStateException("Payment cannot move from " + status + " to " + nextStatus);
        }
        status = nextStatus;
        this.failureReason = nextStatus == PaymentStatus.FAILED ? failureReason : null;
        statusChangedAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public Merchant merchant() {
        return merchant;
    }

    public Invoice invoice() {
        return invoice;
    }

    public String providerReference() {
        return providerReference;
    }

    public PaymentMethod paymentMethod() {
        return paymentMethod;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public PaymentStatus status() {
        return status;
    }

    public String failureReason() {
        return failureReason;
    }
}
