package za.co.safintech.payments.audit.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.safintech.payments.audit.domain.AuditLog;
import za.co.safintech.payments.audit.dto.AuditLogResponse;
import za.co.safintech.payments.audit.repository.AuditLogRepository;

@Service
@Profile("!local")
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogQueryService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listAuditLogs(UUID merchantId) {
        return auditLogRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.id(),
                auditLog.merchantId(),
                auditLog.actorType(),
                auditLog.actorId(),
                auditLog.action(),
                auditLog.targetType(),
                auditLog.targetId(),
                auditLog.previousState(),
                auditLog.newState(),
                auditLog.createdAt());
    }
}
