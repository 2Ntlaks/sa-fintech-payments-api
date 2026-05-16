package za.co.safintech.payments.settlement.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import za.co.safintech.payments.merchant.domain.Merchant;

@Entity
@Table(name = "settlement_batches")
public class SettlementBatch {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SettlementStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    @OneToMany(mappedBy = "settlementBatch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SettlementBatchItem> items = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected SettlementBatch() {
    }

    public SettlementBatch(Merchant merchant) {
        this.id = UUID.randomUUID();
        this.merchant = merchant;
        this.currency = "ZAR";
        this.status = SettlementStatus.CREATED;
        this.grossAmount = money("0.00");
        this.feeAmount = money("0.00");
        this.refundAmount = money("0.00");
        this.netAmount = money("0.00");
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

    public void addItem(SettlementBatchItem item) {
        items.add(item);
        grossAmount = grossAmount.add(item.grossAmount()).setScale(2, RoundingMode.UNNECESSARY);
        feeAmount = feeAmount.add(item.feeAmount()).setScale(2, RoundingMode.UNNECESSARY);
        refundAmount = refundAmount.add(item.refundAmount()).setScale(2, RoundingMode.UNNECESSARY);
        netAmount = netAmount.add(item.netAmount()).setScale(2, RoundingMode.UNNECESSARY);
    }

    public UUID id() {
        return id;
    }

    public Merchant merchant() {
        return merchant;
    }

    public String currency() {
        return currency;
    }

    public SettlementStatus status() {
        return status;
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

    public List<SettlementBatchItem> items() {
        return List.copyOf(items);
    }

    private BigDecimal money(String amount) {
        return new BigDecimal(amount).setScale(2, RoundingMode.UNNECESSARY);
    }
}
