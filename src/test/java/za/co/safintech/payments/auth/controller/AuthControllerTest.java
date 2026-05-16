package za.co.safintech.payments.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import za.co.safintech.payments.auth.dto.AuthResponse;
import za.co.safintech.payments.auth.dto.LoginRequest;
import za.co.safintech.payments.auth.dto.RegisterMerchantRequest;
import za.co.safintech.payments.auth.service.AuthService;
import za.co.safintech.payments.common.api.GlobalExceptionHandler;
import za.co.safintech.payments.common.config.SecurityConfig;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldAllowPublicMerchantRegistration() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(authService.registerMerchant(any(RegisterMerchantRequest.class)))
                .thenReturn(new AuthResponse("token", "Bearer", 3600, merchantId, userId, "OWNER"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessName": "Mpho Tutoring",
                                  "tradingName": "Mpho Maths",
                                  "merchantType": "TUTORING_BUSINESS",
                                  "ownerFullName": "Mpho Dlamini",
                                  "ownerEmail": "mpho@example.com",
                                  "password": "strongPass123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.merchantId").value(merchantId.toString()))
                .andExpect(jsonPath("$.merchantUserId").value(userId.toString()))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void shouldAllowPublicLogin() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("login-token", "Bearer", 3600, merchantId, userId, "OWNER"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "mpho@example.com",
                                  "password": "strongPass123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("login-token"))
                .andExpect(jsonPath("$.merchantId").value(merchantId.toString()));
    }
}
