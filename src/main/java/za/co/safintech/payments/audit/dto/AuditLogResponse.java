package za.co.safintech.payments.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID merchantId,
        String actorType,
        UUID actorId,
        String action,
        String targetType,
        UUID targetId,
        String previousState,
        String newState,
        Instant createdAt) {
}
