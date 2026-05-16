package za.co.safintech.payments.customer.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.safintech.payments.common.exception.ApiException;
import za.co.safintech.payments.customer.domain.Customer;
import za.co.safintech.payments.customer.dto.CreateCustomerRequest;
import za.co.safintech.payments.customer.dto.CustomerResponse;
import za.co.safintech.payments.customer.repository.CustomerRepository;
import za.co.safintech.payments.merchant.domain.Merchant;
import za.co.safintech.payments.merchant.repository.MerchantRepository;

@Service
@Profile("!local")
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;

    public CustomerService(CustomerRepository customerRepository, MerchantRepository merchantRepository) {
        this.customerRepository = customerRepository;
        this.merchantRepository = merchantRepository;
    }

    @Transactional
    public CustomerResponse createCustomer(UUID merchantId, CreateCustomerRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MERCHANT_NOT_FOUND", "Merchant not found"));

        if (request.email() != null && customerRepository.existsByMerchantIdAndEmailIgnoreCase(merchantId, request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "CUSTOMER_EMAIL_ALREADY_EXISTS",
                    "A customer with this email already exists for this merchant");
        }

        Customer customer = customerRepository.save(new Customer(
                merchant,
                request.fullName(),
                request.email(),
                request.phoneNumber()));

        return toResponse(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> listCustomers(UUID merchantId) {
        return customerRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID merchantId, UUID customerId) {
        return customerRepository.findByIdAndMerchantId(customerId, merchantId)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found for this merchant"));
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.id(),
                customer.merchant().id(),
                customer.fullName(),
                customer.email(),
                customer.phoneNumber());
    }
}
