package za.co.safintech.payments.reconciliation.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.safintech.payments.reconciliation.domain.ReconciliationReportItem;

public interface ReconciliationReportItemRepository extends JpaRepository<ReconciliationReportItem, UUID> {
}
