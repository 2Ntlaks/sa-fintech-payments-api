package za.co.safintech.payments.merchant.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import za.co.safintech.payments.common.config.SecurityConfig;
import za.co.safintech.payments.merchant.dto.MerchantProfileResponse;
import za.co.safintech.payments.merchant.service.MerchantProfileService;

@WebMvcTest(MerchantController.class)
@Import(SecurityConfig.class)
class MerchantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MerchantProfileService merchantProfileService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnCurrentMerchantProfileForAuthenticatedUser() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(merchantProfileService.currentMerchantProfile(eq(merchantId), eq(userId)))
                .thenReturn(new MerchantProfileResponse(
                        merchantId,
                        "Mpho Tutoring",
                        "Mpho Maths",
                        "TUTORING_BUSINESS",
                        "ACTIVE",
                        "ZA",
                        "ZAR",
                        userId,
                        "OWNER"));

        mockMvc.perform(get("/api/v1/merchants/me")
                        .with(jwt()
                                .jwt(token -> token
                                        .subject(userId.toString())
                                        .claim("merchant_id", merchantId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId").value(merchantId.toString()))
                .andExpect(jsonPath("$.businessName").value("Mpho Tutoring"))
                .andExpect(jsonPath("$.merchantUserId").value(userId.toString()))
                .andExpect(jsonPath("$.userRole").value("OWNER"));
    }
}
