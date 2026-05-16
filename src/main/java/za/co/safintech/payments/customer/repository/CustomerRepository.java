package za.co.safintech.payments.customer.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import za.co.safintech.payments.customer.domain.Customer;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findAllByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    Optional<Customer> findByIdAndMerchantId(UUID id, UUID merchantId);

    boolean existsByMerchantIdAndEmailIgnoreCase(UUID merchantId, String email);
}
