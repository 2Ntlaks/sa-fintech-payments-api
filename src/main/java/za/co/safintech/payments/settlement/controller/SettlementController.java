package za.co.safintech.payments.settlement.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import za.co.safintech.payments.common.security.JwtMerchantContext;
import za.co.safintech.payments.settlement.dto.SettlementBatchResponse;
import za.co.safintech.payments.settlement.service.SettlementService;

@RestController
@Profile("!local")
@RequestMapping("/api/v1/settlement-batches")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SettlementBatchResponse createSettlementBatch(@AuthenticationPrincipal Jwt jwt) {
        return settlementService.createSettlementBatch(
                JwtMerchantContext.merchantId(jwt),
                JwtMerchantContext.merchantUserId(jwt));
    }

    @GetMapping
    List<SettlementBatchResponse> listSettlementBatches(@AuthenticationPrincipal Jwt jwt) {
        return settlementService.listSettlementBatches(JwtMerchantContext.merchantId(jwt));
    }

    @GetMapping("/{settlementBatchId}")
    SettlementBatchResponse getSettlementBatch(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID settlementBatchId) {
        return settlementService.getSettlementBatch(JwtMerchantContext.merchantId(jwt), settlementBatchId);
    }
}
