package za.co.safintech.payments.invoice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import za.co.safintech.payments.customer.repository.CustomerRepository;
import za.co.safintech.payments.invoice.domain.Invoice;
import za.co.safintech.payments.invoice.dto.CreateInvoiceRequest;
import za.co.safintech.payments.invoice.repository.InvoiceRepository;
import za.co.safintech.payments.merchant.domain.Merchant;
import za.co.safintech.payments.merchant.domain.MerchantType;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(invoiceRepository, customerRepository);
    }

    @Test
    void shouldCreateIssuedZarInvoiceForMerchantOwnedCustomer() {
        Merchant merchant = new Merchant("Mpho Tutoring", null, null, MerchantType.TUTORING_BUSINESS);
        Customer customer = new Customer(merchant, "Student One", "student@example.com", "+27821234567");
        CreateInvoiceRequest request = new CreateInvoiceRequest(customer.id(), "Maths lesson", new BigDecimal("250.00"), LocalDate.now().plusDays(7));

        when(customerRepository.findByIdAndMerchantId(customer.id(), merchant.id())).thenReturn(Optional.of(customer));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = invoiceService.createInvoice(merchant.id(), request);

        assertThat(response.merchantId()).isEqualTo(merchant.id());
        assertThat(response.customerId()).isEqualTo(customer.id());
        assertThat(response.amount()).isEqualByComparingTo("250.00");
        assertThat(response.currency()).isEqualTo("ZAR");
        assertThat(response.status()).isEqualTo("ISSUED");
        verify(customerRepository).findByIdAndMerchantId(customer.id(), merchant.id());
    }

    @Test
    void shouldRejectInvoiceForCustomerOutsideAuthenticatedMerchant() {
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CreateInvoiceRequest request = new CreateInvoiceRequest(customerId, "Wrong merchant", new BigDecimal("100.00"), null);

        when(customerRepository.findByIdAndMerchantId(customerId, merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.createInvoice(merchantId, request))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.code()).isEqualTo("CUSTOMER_NOT_FOUND");
                });
    }

    @Test
    void shouldRejectInvoiceAmountWithMoreThanTwoDecimalPlaces() {
        Merchant merchant = new Merchant("Mpho Tutoring", null, null, MerchantType.TUTORING_BUSINESS);
        Customer customer = new Customer(merchant, "Student One", "student@example.com", "+27821234567");
        CreateInvoiceRequest request = new CreateInvoiceRequest(customer.id(), "Bad scale", new BigDecimal("100.999"), null);

        when(customerRepository.findByIdAndMerchantId(customer.id(), merchant.id())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> invoiceService.createInvoice(merchant.id(), request))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.code()).isEqualTo("INVALID_MONEY_SCALE");
                });
    }

    @Test
    void shouldCancelIssuedInvoice() {
        Merchant merchant = new Merchant("Mpho Tutoring", null, null, MerchantType.TUTORING_BUSINESS);
        Customer customer = new Customer(merchant, "Student One", "student@example.com", "+27821234567");
        Invoice invoice = new Invoice(merchant, customer, "INV-TEST123", "Lesson", new BigDecimal("250.00"), null);

        when(invoiceRepository.findByIdAndMerchantId(invoice.id(), merchant.id())).thenReturn(Optional.of(invoice));

        var response = invoiceService.cancelInvoice(merchant.id(), invoice.id());

        assertThat(response.status()).isEqualTo("CANCELLED");
        verify(invoiceRepository).findByIdAndMerchantId(invoice.id(), merchant.id());
    }

    @Test
    void shouldRejectCancellingPaidInvoice() {
        Merchant merchant = new Merchant("Mpho Tutoring", null, null, MerchantType.TUTORING_BUSINESS);
        Customer customer = new Customer(merchant, "Student One", "student@example.com", "+27821234567");
        Invoice invoice = new Invoice(merchant, customer, "INV-TEST123", "Lesson", new BigDecimal("250.00"), null);
        invoice.markPaid();

        when(invoiceRepository.findByIdAndMerchantId(invoice.id(), merchant.id())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.cancelInvoice(merchant.id(), invoice.id()))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.code()).isEqualTo("INVALID_INVOICE_STATUS");
                });
    }
}
