package za.co.safintech.payments.merchant.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.safintech.payments.auth.repository.MerchantUserRepository;
import za.co.safintech.payments.common.exception.ApiException;
import za.co.safintech.payments.merchant.domain.Merchant;
import za.co.safintech.payments.merchant.dto.MerchantProfileResponse;
import za.co.safintech.payments.merchant.repository.MerchantRepository;

@Service
@Profile("!local")
public class MerchantProfileService {

    private final MerchantRepository merchantRepository;
    private final MerchantUserRepository merchantUserRepository;

    public MerchantProfileService(MerchantRepository merchantRepository, MerchantUserRepository merchantUserRepository) {
        this.merchantRepository = merchantRepository;
        this.merchantUserRepository = merchantUserRepository;
    }

    @Transactional(readOnly = true)
    public MerchantProfileResponse currentMerchantProfile(UUID merchantId, UUID merchantUserId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MERCHANT_NOT_FOUND", "Merchant not found"));

        var user = merchantUserRepository.findById(merchantUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MERCHANT_USER_NOT_FOUND", "Merchant user not found"));

        if (!user.merchant().id().equals(merchant.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MERCHANT_ACCESS_DENIED",
                    "Merchant user does not belong to this merchant");
        }

        return new MerchantProfileResponse(
                merchant.id(),
                merchant.businessName(),
                merchant.tradingName(),
                merchant.merchantType().name(),
                merchant.status().name(),
                merchant.countryCode(),
                merchant.defaultCurrency(),
                user.id(),
                user.role().name());
    }
}
