package za.co.safintech.payments.customer.dto;

import java.util.UUID;

public record CustomerResponse(
        UUID id,
        UUID merchantId,
        String fullName,
        String email,
        String phoneNumber) {
}
