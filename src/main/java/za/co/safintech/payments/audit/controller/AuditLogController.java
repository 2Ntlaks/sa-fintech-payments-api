package za.co.safintech.payments.audit.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.co.safintech.payments.audit.dto.AuditLogResponse;
import za.co.safintech.payments.audit.service.AuditLogQueryService;
import za.co.safintech.payments.common.security.JwtMerchantContext;

@RestController
@Profile("!local")
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    public AuditLogController(AuditLogQueryService auditLogQueryService) {
        this.auditLogQueryService = auditLogQueryService;
    }

    @GetMapping
    List<AuditLogResponse> listAuditLogs(@AuthenticationPrincipal Jwt jwt) {
        return auditLogQueryService.listAuditLogs(JwtMerchantContext.merchantId(jwt));
    }
}
