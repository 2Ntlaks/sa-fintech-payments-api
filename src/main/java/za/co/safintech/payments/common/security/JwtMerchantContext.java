package za.co.safintech.payments.common.security;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import za.co.safintech.payments.common.exception.ApiException;

public final class JwtMerchantContext {

    private JwtMerchantContext() {
    }

    public static UUID merchantId(Jwt jwt) {
        return uuidClaim(jwt, "merchant_id");
    }

    public static UUID merchantUserId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw invalidTokenClaims();
        }
    }

    private static UUID uuidClaim(Jwt jwt, String claimName) {
        try {
            String value = jwt.getClaimAsString(claimName);
            if (value == null || value.isBlank()) {
                throw invalidTokenClaims();
            }
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw invalidTokenClaims();
        }
    }

    private static ApiException invalidTokenClaims() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN_CLAIMS",
                "Authenticated token is missing required merchant context");
    }
}
