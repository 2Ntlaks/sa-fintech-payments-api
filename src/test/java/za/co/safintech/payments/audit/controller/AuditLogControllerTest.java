package za.co.safintech.payments.audit.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import za.co.safintech.payments.audit.dto.AuditLogResponse;
import za.co.safintech.payments.audit.service.AuditLogQueryService;
import za.co.safintech.payments.common.config.SecurityConfig;

@WebMvcTest(AuditLogController.class)
@Import(SecurityConfig.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogQueryService auditLogQueryService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnMerchantScopedAuditLogsForAuthenticatedUser() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID auditLogId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(auditLogQueryService.listAuditLogs(eq(merchantId)))
                .thenReturn(List.of(new AuditLogResponse(
                        auditLogId,
                        merchantId,
                        "MERCHANT_USER",
                        actorId,
                        "PAYMENT_STATUS_CHANGED",
                        "PAYMENT_ATTEMPT",
                        targetId,
                        "PROCESSING",
                        "SUCCEEDED",
                        Instant.parse("2026-05-16T20:00:00Z"))));

        mockMvc.perform(get("/api/v1/audit-logs")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject(actorId.toString())
                                        .claim("merchant_id", merchantId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(auditLogId.toString()))
                .andExpect(jsonPath("$[0].merchantId").value(merchantId.toString()))
                .andExpect(jsonPath("$[0].action").value("PAYMENT_STATUS_CHANGED"));
    }

    @Test
    void shouldRequireAuthenticationForAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isUnauthorized());
    }
}
