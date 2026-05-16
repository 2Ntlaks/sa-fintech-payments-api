package za.co.safintech.payments.payment.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import za.co.safintech.payments.payment.domain.PaymentAttempt;
import za.co.safintech.payments.payment.domain.PaymentStatus;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

    List<PaymentAttempt> findAllByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    Optional<PaymentAttempt> findByIdAndMerchantId(UUID id, UUID merchantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentAttempt> findWithLockByIdAndMerchantId(UUID id, UUID merchantId);

    boolean existsByInvoiceIdAndStatus(UUID invoiceId, PaymentStatus status);
}
