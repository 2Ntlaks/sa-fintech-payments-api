package za.co.safintech.payments.customer.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import za.co.safintech.payments.merchant.domain.Merchant;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(length = 320)
    private String email;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Customer() {
    }

    public Customer(Merchant merchant, String fullName, String email, String phoneNumber) {
        this.id = UUID.randomUUID();
        this.merchant = merchant;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
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

    public String phoneNumber() {
        return phoneNumber;
    }
}
