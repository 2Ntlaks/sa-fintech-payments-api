package za.co.safintech.payments.common.api;

import java.time.Instant;

public record HealthResponse(String status, String service, Instant timestamp) {
}
