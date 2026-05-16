package za.co.safintech.payments.merchant.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.safintech.payments.merchant.domain.Merchant;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
}
