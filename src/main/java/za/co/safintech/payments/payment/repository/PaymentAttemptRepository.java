package za.co.safintech.payments.payment.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import za.co.safintech.payments.payment.domain.PaymentAttempt;
import za.co.safintech.payments.payment.domain.PaymentStatus;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

    List<PaymentAttempt> findAllByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    Optional<PaymentAttempt> findByIdAndMerchantId(UUID id, UUID merchantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentAttempt> findWithLockByIdAndMerchantId(UUID id, UUID merchantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentAttempt> findWithLockByProviderReference(String providerReference);

    boolean existsByInvoiceIdAndStatus(UUID invoiceId, PaymentStatus status);

    boolean existsByInvoiceIdAndStatusIn(UUID invoiceId, List<PaymentStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select paymentAttempt
            from PaymentAttempt paymentAttempt
            where paymentAttempt.merchant.id = :merchantId
              and paymentAttempt.status in :statuses
              and not exists (
                  select item.id
                  from SettlementBatchItem item
                  where item.paymentAttempt.id = paymentAttempt.id
              )
            order by paymentAttempt.createdAt asc
            """)
    List<PaymentAttempt> findEligibleForSettlementWithLock(
            @Param("merchantId") UUID merchantId,
            @Param("statuses") List<PaymentStatus> statuses);
}
