package za.co.safintech.payments.payment.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Column(nullable = false, length = 80)
    private String operation;

    @Column(nullable = false, length = 120)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String requestHash;

    @Column(nullable = false, length = 80)
    private String targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Column(nullable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(UUID merchantId, String operation, String idempotencyKey,
            String requestHash, String targetType, UUID targetId) {
        this.id = UUID.randomUUID();
        this.merchantId = merchantId;
        this.operation = operation;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public String requestHash() {
        return requestHash;
    }

    public UUID targetId() {
        return targetId;
    }
}
