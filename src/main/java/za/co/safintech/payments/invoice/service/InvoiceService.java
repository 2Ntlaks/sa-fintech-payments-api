package za.co.safintech.payments.invoice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.safintech.payments.common.exception.ApiException;
import za.co.safintech.payments.customer.domain.Customer;
import za.co.safintech.payments.customer.repository.CustomerRepository;
import za.co.safintech.payments.invoice.domain.Invoice;
import za.co.safintech.payments.invoice.dto.CreateInvoiceRequest;
import za.co.safintech.payments.invoice.dto.InvoiceResponse;
import za.co.safintech.payments.invoice.repository.InvoiceRepository;

@Service
@Profile("!local")
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, CustomerRepository customerRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public InvoiceResponse createInvoice(UUID merchantId, CreateInvoiceRequest request) {
        Customer customer = customerRepository.findByIdAndMerchantId(request.customerId(), merchantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found for this merchant"));

        BigDecimal amount = normalizeAmount(request.amount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INVOICE_AMOUNT", "Invoice amount must be greater than zero");
        }
        if (request.dueDate() != null && request.dueDate().isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DUE_DATE", "Invoice due date cannot be in the past");
        }

        Invoice invoice = invoiceRepository.save(new Invoice(
                customer.merchant(),
                customer,
                nextInvoiceNumber(),
                request.description(),
                amount,
                request.dueDate()));

        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices(UUID merchantId) {
        return invoiceRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID merchantId, UUID invoiceId) {
        return invoiceRepository.findByIdAndMerchantId(invoiceId, merchantId)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found for this merchant"));
    }

    @Transactional
    public InvoiceResponse cancelInvoice(UUID merchantId, UUID invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndMerchantId(invoiceId, merchantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found for this merchant"));

        try {
            invoice.cancel();
        } catch (IllegalStateException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_INVOICE_STATUS", exception.getMessage());
        }

        return toResponse(invoice);
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.id(),
                invoice.merchant().id(),
                invoice.customer().id(),
                invoice.invoiceNumber(),
                invoice.description(),
                invoice.amount(),
                invoice.currency(),
                invoice.status().name(),
                invoice.dueDate());
    }

    private String nextInvoiceNumber() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MONEY_SCALE",
                    "Invoice amount must have no more than two decimal places");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }
}
