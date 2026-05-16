package za.co.safintech.payments.invoice.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
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
import za.co.safintech.payments.customer.domain.Customer;
import za.co.safintech.payments.merchant.domain.Merchant;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 40)
    private String invoiceNumber;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private InvoiceStatus status;

    private LocalDate dueDate;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Invoice() {
    }

    public Invoice(Merchant merchant, Customer customer, String invoiceNumber, String description, BigDecimal amount, LocalDate dueDate) {
        this.id = UUID.randomUUID();
        this.merchant = merchant;
        this.customer = customer;
        this.invoiceNumber = invoiceNumber;
        this.description = description;
        this.amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        this.currency = "ZAR";
        this.status = InvoiceStatus.ISSUED;
        this.dueDate = dueDate;
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

    public void cancel() {
        if (status != InvoiceStatus.ISSUED) {
            throw new IllegalStateException("Only issued invoices can be cancelled");
        }
        status = InvoiceStatus.CANCELLED;
    }

    public void markPaid() {
        if (status != InvoiceStatus.ISSUED) {
            throw new IllegalStateException("Only issued invoices can be marked paid");
        }
        status = InvoiceStatus.PAID;
    }

    public void applySuccessfulRefundTotal(BigDecimal successfulRefundTotal) {
        if (status != InvoiceStatus.PAID && status != InvoiceStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("Only paid invoices can be refunded");
        }
        BigDecimal normalizedTotal = successfulRefundTotal.setScale(2, RoundingMode.UNNECESSARY);
        status = normalizedTotal.compareTo(amount) == 0
                ? InvoiceStatus.REFUNDED
                : InvoiceStatus.PARTIALLY_REFUNDED;
    }

    public UUID id() {
        return id;
    }

    public Merchant merchant() {
        return merchant;
    }

    public Customer customer() {
        return customer;
    }

    public String invoiceNumber() {
        return invoiceNumber;
    }

    public String description() {
        return description;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public InvoiceStatus status() {
        return status;
    }

    public LocalDate dueDate() {
        return dueDate;
    }
}
