package za.co.safintech.payments.merchant.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String businessName;

    @Column(length = 160)
    private String tradingName;

    @Column(length = 80)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MerchantType merchantType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MerchantStatus status;

    @Column(nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, length = 3)
    private String defaultCurrency;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Merchant() {
    }

    public Merchant(String businessName, String tradingName, String registrationNumber, MerchantType merchantType) {
        this.id = UUID.randomUUID();
        this.businessName = businessName;
        this.tradingName = tradingName;
        this.registrationNumber = registrationNumber;
        this.merchantType = merchantType;
        this.status = MerchantStatus.ACTIVE;
        this.countryCode = "ZA";
        this.defaultCurrency = "ZAR";
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

    public String businessName() {
        return businessName;
    }

    public String tradingName() {
        return tradingName;
    }

    public String registrationNumber() {
        return registrationNumber;
    }

    public MerchantType merchantType() {
        return merchantType;
    }

    public MerchantStatus status() {
        return status;
    }

    public String countryCode() {
        return countryCode;
    }

    public String defaultCurrency() {
        return defaultCurrency;
    }
}
