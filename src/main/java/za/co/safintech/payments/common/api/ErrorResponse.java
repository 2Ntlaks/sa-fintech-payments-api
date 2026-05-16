package za.co.safintech.payments.common.api;

import java.time.Instant;

public record ErrorResponse(String code, String message, Instant timestamp) {
}
