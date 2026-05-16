package za.co.safintech.payments.refund.controller;

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
import za.co.safintech.payments.refund.dto.CreateRefundRequest;
import za.co.safintech.payments.refund.dto.RefundResponse;
import za.co.safintech.payments.refund.service.RefundService;

@RestController
@Profile("!local")
@RequestMapping("/api/v1/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RefundResponse createRefund(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateRefundRequest request) {
        return refundService.createRefund(
                JwtMerchantContext.merchantId(jwt),
                JwtMerchantContext.merchantUserId(jwt),
                request);
    }

    @GetMapping
    List<RefundResponse> listRefunds(@AuthenticationPrincipal Jwt jwt) {
        return refundService.listRefunds(JwtMerchantContext.merchantId(jwt));
    }

    @GetMapping("/{refundId}")
    RefundResponse getRefund(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID refundId) {
        return refundService.getRefund(JwtMerchantContext.merchantId(jwt), refundId);
    }
}
