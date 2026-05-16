package za.co.safintech.payments.balance.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import za.co.safintech.payments.balance.domain.MerchantBalance;

public interface MerchantBalanceRepository extends JpaRepository<MerchantBalance, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MerchantBalance> findWithLockByMerchantId(UUID merchantId);
}
