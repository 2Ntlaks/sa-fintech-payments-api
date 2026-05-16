package za.co.safintech.payments.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import za.co.safintech.payments.common.exception.ApiException;
import za.co.safintech.payments.customer.domain.Customer;
import za.co.safintech.payments.customer.dto.CreateCustomerRequest;
import za.co.safintech.payments.customer.repository.CustomerRepository;
import za.co.safintech.payments.merchant.domain.Merchant;
import za.co.safintech.payments.merchant.domain.MerchantType;
import za.co.safintech.payments.merchant.repository.MerchantRepository;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private MerchantRepository merchantRepository;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, merchantRepository);
    }

    @Test
    void shouldCreateCustomerForAuthenticatedMerchant() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        CreateCustomerRequest request = new CreateCustomerRequest("Lerato Khumalo", "lerato@example.com", "+27821234567");

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));
        when(customerRepository.existsByMerchantIdAndEmailIgnoreCase(merchant.id(), "lerato@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = customerService.createCustomer(merchant.id(), request);

        assertThat(response.merchantId()).isEqualTo(merchant.id());
        assertThat(response.fullName()).isEqualTo("Lerato Khumalo");
        assertThat(response.phoneNumber()).isEqualTo("+27821234567");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void shouldRejectDuplicateCustomerEmailForSameMerchant() {
        Merchant merchant = new Merchant("Naledi Spaza", null, null, MerchantType.SPAZA_SHOP);
        CreateCustomerRequest request = new CreateCustomerRequest("Lerato Khumalo", "lerato@example.com", "+27821234567");

        when(merchantRepository.findById(merchant.id())).thenReturn(Optional.of(merchant));
        when(customerRepository.existsByMerchantIdAndEmailIgnoreCase(merchant.id(), "lerato@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(merchant.id(), request))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.code()).isEqualTo("CUSTOMER_EMAIL_ALREADY_EXISTS");
                });
    }

    @Test
    void shouldLookupCustomerByCustomerIdAndMerchantId() {
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        when(customerRepository.findByIdAndMerchantId(customerId, merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomer(merchantId, customerId))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.code()).isEqualTo("CUSTOMER_NOT_FOUND");
                });

        verify(customerRepository).findByIdAndMerchantId(customerId, merchantId);
    }
}
