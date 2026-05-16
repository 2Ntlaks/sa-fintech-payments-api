package za.co.safintech.payments.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import za.co.safintech.payments.auth.domain.MerchantUser;
import za.co.safintech.payments.auth.dto.LoginRequest;
import za.co.safintech.payments.auth.dto.RegisterMerchantRequest;
import za.co.safintech.payments.auth.repository.MerchantUserRepository;
import za.co.safintech.payments.common.exception.ApiException;
import za.co.safintech.payments.merchant.domain.Merchant;
import za.co.safintech.payments.merchant.domain.MerchantType;
import za.co.safintech.payments.merchant.repository.MerchantRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private MerchantUserRepository merchantUserRepository;

    @Mock
    private JwtService jwtService;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(merchantRepository, merchantUserRepository, passwordEncoder, jwtService);
    }

    @Test
    void shouldRegisterMerchantOwnerAndReturnToken() {
        RegisterMerchantRequest request = new RegisterMerchantRequest(
                "Mpho Tutoring",
                "Mpho Maths",
                null,
                MerchantType.TUTORING_BUSINESS,
                "Mpho Dlamini",
                "MPHO@example.com",
                "strongPass123");
        Merchant merchant = new Merchant("Mpho Tutoring", "Mpho Maths", null, MerchantType.TUTORING_BUSINESS);

        when(merchantUserRepository.existsByEmailIgnoreCase("MPHO@example.com")).thenReturn(false);
        when(merchantRepository.save(any(Merchant.class))).thenReturn(merchant);
        when(merchantUserRepository.save(any(MerchantUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.createToken(any(MerchantUser.class))).thenReturn(new JwtService.GeneratedToken("token-value", 3600));

        var response = authService.registerMerchant(request);

        assertThat(response.accessToken()).isEqualTo("token-value");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.merchantId()).isEqualTo(merchant.id());
        assertThat(response.role()).isEqualTo("OWNER");
        verify(merchantRepository).save(any(Merchant.class));
        verify(merchantUserRepository).save(any(MerchantUser.class));
    }

    @Test
    void shouldRejectDuplicateOwnerEmailDuringRegistration() {
        RegisterMerchantRequest request = new RegisterMerchantRequest(
                "Kasi Spaza",
                null,
                null,
                MerchantType.SPAZA_SHOP,
                "Naledi Mokoena",
                "naledi@example.com",
                "strongPass123");

        when(merchantUserRepository.existsByEmailIgnoreCase("naledi@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerMerchant(request))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.code()).isEqualTo("EMAIL_ALREADY_REGISTERED");
                });
    }

    @Test
    void shouldLoginWithValidCredentials() {
        Merchant merchant = new Merchant("Online Course ZA", null, null, MerchantType.ONLINE_COURSE_SELLER);
        MerchantUser user = new MerchantUser(
                merchant,
                "Aisha Khan",
                "aisha@example.com",
                passwordEncoder.encode("correct-password"));

        when(merchantUserRepository.findByEmailIgnoreCase("aisha@example.com")).thenReturn(java.util.Optional.of(user));
        when(jwtService.createToken(user)).thenReturn(new JwtService.GeneratedToken("login-token", 3600));

        var response = authService.login(new LoginRequest("aisha@example.com", "correct-password"));

        assertThat(response.accessToken()).isEqualTo("login-token");
        assertThat(response.merchantId()).isEqualTo(merchant.id());
        assertThat(response.merchantUserId()).isEqualTo(user.id());
    }

    @Test
    void shouldRejectInvalidLoginPassword() {
        Merchant merchant = new Merchant("Small Retailer", null, null, MerchantType.SMALL_RETAILER);
        MerchantUser user = new MerchantUser(
                merchant,
                "Thabo Ndlovu",
                "thabo@example.com",
                passwordEncoder.encode("correct-password"));

        when(merchantUserRepository.findByEmailIgnoreCase("thabo@example.com")).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("thabo@example.com", "wrong-password")))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(apiException.code()).isEqualTo("INVALID_CREDENTIALS");
                });
    }
}
