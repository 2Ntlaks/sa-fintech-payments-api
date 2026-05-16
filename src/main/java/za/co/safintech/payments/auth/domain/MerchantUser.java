package za.co.safintech.payments.auth.domain;

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

@Entity
@Table(name = "merchant_users")
public class MerchantUser {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MerchantUserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MerchantUserStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected MerchantUser() {
    }

    public MerchantUser(Merchant merchant, String fullName, String email, String passwordHash) {
        this.id = UUID.randomUUID();
        this.merchant = merchant;
        this.fullName = fullName;
        this.email = email.toLowerCase();
        this.passwordHash = passwordHash;
        this.role = MerchantUserRole.OWNER;
        this.status = MerchantUserStatus.ACTIVE;
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

    public UUID id() {
        return id;
    }

    public Merchant merchant() {
        return merchant;
    }

    public String fullName() {
        return fullName;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public MerchantUserRole role() {
        return role;
    }

    public MerchantUserStatus status() {
        return status;
    }
}
