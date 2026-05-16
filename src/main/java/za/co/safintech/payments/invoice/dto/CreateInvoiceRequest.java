package za.co.safintech.payments.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateInvoiceRequest(
        @NotNull UUID customerId,
        @Size(max = 255) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        LocalDate dueDate) {
}
