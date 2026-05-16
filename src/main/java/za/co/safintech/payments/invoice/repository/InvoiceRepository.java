package za.co.safintech.payments.invoice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.safintech.payments.invoice.domain.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findAllByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    Optional<Invoice> findByIdAndMerchantId(UUID id, UUID merchantId);
}
