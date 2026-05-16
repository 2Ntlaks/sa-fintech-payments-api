package za.co.safintech.payments.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.safintech.payments.auth.domain.MerchantUser;

public interface MerchantUserRepository extends JpaRepository<MerchantUser, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<MerchantUser> findByEmailIgnoreCase(String email);
}
