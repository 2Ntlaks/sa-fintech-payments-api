package za.co.safintech.payments.merchant.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import za.co.safintech.payments.common.security.JwtMerchantContext;
import za.co.safintech.payments.merchant.dto.MerchantProfileResponse;
import za.co.safintech.payments.merchant.service.MerchantProfileService;

@RestController
@Profile("!local")
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final MerchantProfileService merchantProfileService;

    public MerchantController(MerchantProfileService merchantProfileService) {
        this.merchantProfileService = merchantProfileService;
    }

    @GetMapping("/me")
    MerchantProfileResponse currentMerchant(@AuthenticationPrincipal Jwt jwt) {
        return merchantProfileService.currentMerchantProfile(
                JwtMerchantContext.merchantId(jwt),
                JwtMerchantContext.merchantUserId(jwt));
    }
}
