package za.co.safintech.payments.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.safintech.payments.audit.domain.AuditLog;
import za.co.safintech.payments.audit.repository.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditLogQueryServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogQueryService auditLogQueryService;

    @BeforeEach
    void setUp() {
        auditLogQueryService = new AuditLogQueryService(auditLogRepository);
    }

    @Test
    void shouldReturnOnlyMerchantScopedAuditLogs() {
        UUID merchantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        AuditLog auditLog = new AuditLog(
                merchantId,
                "MERCHANT_USER",
                actorId,
                "PAYMENT_STATUS_CHANGED",
                "PAYMENT_ATTEMPT",
                targetId,
                "PROCESSING",
                "SUCCEEDED");

        when(auditLogRepository.findAllByMerchantIdOrderByCreatedAtDesc(merchantId)).thenReturn(List.of(auditLog));

        var response = auditLogQueryService.listAuditLogs(merchantId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).merchantId()).isEqualTo(merchantId);
        assertThat(response.get(0).actorId()).isEqualTo(actorId);
        assertThat(response.get(0).targetId()).isEqualTo(targetId);
        assertThat(response.get(0).action()).isEqualTo("PAYMENT_STATUS_CHANGED");
    }
}
