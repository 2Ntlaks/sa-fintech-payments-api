package za.co.safintech.payments.audit.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    private UUID id;

    private UUID merchantId;

    @Column(nullable = false, length = 40)
    private String actorType;

    private UUID actorId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 80)
    private String targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Column(length = 80)
    private String previousState;

    @Column(length = 80)
    private String newState;

    @Column(nullable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(UUID merchantId, String actorType, UUID actorId, String action, String targetType,
            UUID targetId, String previousState, String newState) {
        this.id = UUID.randomUUID();
        this.merchantId = merchantId;
        this.actorType = actorType;
        this.actorId = actorId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.previousState = previousState;
        this.newState = newState;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
