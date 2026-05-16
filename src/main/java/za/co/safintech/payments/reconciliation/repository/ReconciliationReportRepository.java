package za.co.safintech.payments.reconciliation.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.safintech.payments.reconciliation.domain.ReconciliationReport;

public interface ReconciliationReportRepository extends JpaRepository<ReconciliationReport, UUID> {

    List<ReconciliationReport> findAllByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    Optional<ReconciliationReport> findByIdAndMerchantId(UUID id, UUID merchantId);
}
