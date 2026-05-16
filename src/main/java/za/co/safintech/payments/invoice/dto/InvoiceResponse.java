package za.co.safintech.payments.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID merchantId,
        UUID customerId,
        String invoiceNumber,
        String description,
        BigDecimal amount,
        String currency,
        String status,
        LocalDate dueDate) {
}
