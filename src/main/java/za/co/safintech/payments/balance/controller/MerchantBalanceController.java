package za.co.safintech.payments.balance.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.co.safintech.payments.balance.dto.MerchantBalanceResponse;
import za.co.safintech.payments.balance.service.MerchantBalanceService;
import za.co.safintech.payments.common.security.JwtMerchantContext;

@RestController
@Profile("!local")
@RequestMapping("/api/v1/merchant-balance")
public class MerchantBalanceController {

    private final MerchantBalanceService merchantBalanceService;

    public MerchantBalanceController(MerchantBalanceService merchantBalanceService) {
        this.merchantBalanceService = merchantBalanceService;
    }

    @GetMapping
    MerchantBalanceResponse currentBalance(@AuthenticationPrincipal Jwt jwt) {
        return merchantBalanceService.getBalance(JwtMerchantContext.merchantId(jwt));
    }
}
