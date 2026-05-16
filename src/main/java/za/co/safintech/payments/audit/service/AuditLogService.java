package za.co.safintech.payments.audit.service;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import za.co.safintech.payments.audit.domain.AuditLog;
import za.co.safintech.payments.audit.repository.AuditLogRepository;

@Service
@Profile("!local")
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(UUID merchantId, UUID actorId, String action, String targetType,
            UUID targetId, String previousState, String newState) {
        auditLogRepository.save(new AuditLog(
                merchantId,
                "MERCHANT_USER",
                actorId,
                action,
                targetType,
                targetId,
                previousState,
                newState));
    }
}
