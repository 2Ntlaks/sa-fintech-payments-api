package za.co.safintech.payments.reconciliation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import za.co.safintech.payments.common.security.JwtMerchantContext;
import za.co.safintech.payments.reconciliation.dto.CreateReconciliationReportRequest;
import za.co.safintech.payments.reconciliation.dto.ReconciliationReportResponse;
import za.co.safintech.payments.reconciliation.service.ReconciliationService;

@RestController
@Profile("!local")
@RequestMapping("/api/v1/reconciliation-reports")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ReconciliationReportResponse createReport(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateReconciliationReportRequest request) {
        return reconciliationService.createReport(
                JwtMerchantContext.merchantId(jwt),
                JwtMerchantContext.merchantUserId(jwt),
                request);
    }

    @GetMapping
    List<ReconciliationReportResponse> listReports(@AuthenticationPrincipal Jwt jwt) {
        return reconciliationService.listReports(JwtMerchantContext.merchantId(jwt));
    }

    @GetMapping("/{reportId}")
    ReconciliationReportResponse getReport(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reportId) {
        return reconciliationService.getReport(JwtMerchantContext.merchantId(jwt), reportId);
    }
}
