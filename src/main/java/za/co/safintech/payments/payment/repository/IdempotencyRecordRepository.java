package za.co.safintech.payments.payment.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.safintech.payments.payment.domain.IdempotencyRecord;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByMerchantIdAndOperationAndIdempotencyKey(
            UUID merchantId, String operation, String idempotencyKey);
}
