package za.co.safintech.payments.settlement.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.safintech.payments.settlement.domain.SettlementBatch;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, UUID> {

    List<SettlementBatch> findAllByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    Optional<SettlementBatch> findByIdAndMerchantId(UUID id, UUID merchantId);
}
