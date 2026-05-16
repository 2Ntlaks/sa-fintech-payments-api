package za.co.safintech.payments.settlement.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.safintech.payments.settlement.domain.SettlementBatchItem;

public interface SettlementBatchItemRepository extends JpaRepository<SettlementBatchItem, UUID> {

    boolean existsByPaymentAttemptId(UUID paymentAttemptId);
}
