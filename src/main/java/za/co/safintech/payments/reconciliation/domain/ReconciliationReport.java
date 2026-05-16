package za.co.safintech.payments.reconciliation.domain;

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
import jakarta.persistence.Table;
import za.co.safintech.payments.merchant.domain.Merchant;

@Entity
@Table(name = "reconciliation_reports")
public class ReconciliationReport {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReconciliationReportStatus status;

    @Column(nullable = false)
    private int totalRecords;

    @Column(nullable = false)
    private int matchedCount;

    @Column(nullable = false)
    private int exceptionCount;

    @OneToMany(mappedBy = "reconciliationReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReconciliationReportItem> items = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    protected ReconciliationReport() {
    }

    public ReconciliationReport(Merchant merchant) {
        this.id = UUID.randomUUID();
        this.merchant = merchant;
        this.status = ReconciliationReportStatus.COMPLETED;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public void addItem(ReconciliationReportItem item) {
        items.add(item);
        totalRecords++;
        if (item.resultType() == ReconciliationResultType.MATCHED) {
            matchedCount++;
        } else {
            exceptionCount++;
        }
    }

    public UUID id() {
        return id;
    }

    public Merchant merchant() {
        return merchant;
    }

    public ReconciliationReportStatus status() {
        return status;
    }

    public int totalRecords() {
        return totalRecords;
    }

    public int matchedCount() {
        return matchedCount;
    }

    public int exceptionCount() {
        return exceptionCount;
    }

    public List<ReconciliationReportItem> items() {
        return List.copyOf(items);
    }
}
