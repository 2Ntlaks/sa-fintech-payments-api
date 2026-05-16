package za.co.safintech.payments.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.safintech.payments.auth.domain.MerchantUser;
import za.co.safintech.payments.auth.dto.AuthResponse;
import za.co.safintech.payments.auth.dto.LoginRequest;
import za.co.safintech.payments.auth.dto.RegisterMerchantRequest;
import za.co.safintech.payments.auth.repository.MerchantUserRepository;
import za.co.safintech.payments.common.exception.ApiException;
import za.co.safintech.payments.merchant.domain.Merchant;
import za.co.safintech.payments.merchant.repository.MerchantRepository;

@Service
@Profile("!local")
public class AuthService {

    private final MerchantRepository merchantRepository;
    private final MerchantUserRepository merchantUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            MerchantRepository merchantRepository,
            MerchantUserRepository merchantUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.merchantRepository = merchantRepository;
        this.merchantUserRepository = merchantUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse registerMerchant(RegisterMerchantRequest request) {
        if (merchantUserRepository.existsByEmailIgnoreCase(request.ownerEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED",
                    "A merchant user with this email already exists");
        }

        Merchant merchant = merchantRepository.save(new Merchant(
                request.businessName(),
                request.tradingName(),
                request.registrationNumber(),
                request.merchantType()));

        MerchantUser owner = merchantUserRepository.save(new MerchantUser(
                merchant,
                request.ownerFullName(),
                request.ownerEmail(),
                passwordEncoder.encode(request.password())));

        return authResponse(owner);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        MerchantUser user = merchantUserRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> invalidCredentials());

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw invalidCredentials();
        }

        return authResponse(user);
    }

    private AuthResponse authResponse(MerchantUser user) {
        JwtService.GeneratedToken token = jwtService.createToken(user);
        return new AuthResponse(
                token.value(),
                "Bearer",
                token.expiresInSeconds(),
                user.merchant().id(),
                user.id(),
                user.role().name());
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
    }
}
