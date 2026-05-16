package za.co.safintech.payments.refund.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import za.co.safintech.payments.refund.domain.Refund;
import za.co.safintech.payments.refund.domain.RefundStatus;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    List<Refund> findAllByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    Optional<Refund> findByIdAndMerchantId(UUID id, UUID merchantId);

    @Query("""
            select coalesce(sum(refund.amount), 0)
            from Refund refund
            where refund.paymentAttempt.id = :paymentAttemptId
              and refund.status = :status
            """)
    BigDecimal sumAmountByPaymentAttemptIdAndStatus(
            @Param("paymentAttemptId") UUID paymentAttemptId,
            @Param("status") RefundStatus status);
}
