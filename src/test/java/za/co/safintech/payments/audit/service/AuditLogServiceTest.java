package za.co.safintech.payments.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.safintech.payments.audit.domain.AuditLog;
import za.co.safintech.payments.audit.repository.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(auditLogRepository);
    }

    @Test
    void shouldRecordOnlyStructuredStateValuesWithoutSensitivePayloads() {
        UUID merchantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        auditLogService.record(merchantId, actorId, "REFUND_SUCCEEDED", "REFUND",
                targetId, "REQUESTED", "SUCCEEDED");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.merchantId()).isEqualTo(merchantId);
        assertThat(saved.actorId()).isEqualTo(actorId);
        assertThat(saved.action()).isEqualTo("REFUND_SUCCEEDED");
        assertThat(saved.previousState()).isEqualTo("REQUESTED");
        assertThat(saved.newState()).isEqualTo("SUCCEEDED");
        assertThat(saved.previousState()).doesNotContainIgnoringCase("password", "bearer", "secret", "token");
        assertThat(saved.newState()).doesNotContainIgnoringCase("password", "bearer", "secret", "token");
    }
}
