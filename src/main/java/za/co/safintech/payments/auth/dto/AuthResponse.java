package za.co.safintech.payments.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UUID merchantId,
        UUID merchantUserId,
        String role) {
}
