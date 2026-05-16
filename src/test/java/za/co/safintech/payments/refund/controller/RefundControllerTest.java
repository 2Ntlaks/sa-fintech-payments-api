package za.co.safintech.payments.refund.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import za.co.safintech.payments.common.api.GlobalExceptionHandler;
import za.co.safintech.payments.common.config.SecurityConfig;
import za.co.safintech.payments.refund.dto.CreateRefundRequest;
import za.co.safintech.payments.refund.dto.RefundResponse;
import za.co.safintech.payments.refund.service.RefundService;

@WebMvcTest(RefundController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class RefundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RefundService refundService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldCreateRefundForAuthenticatedMerchant() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();

        when(refundService.createRefund(eq(merchantId), eq(userId), any(CreateRefundRequest.class)))
                .thenReturn(refundResponse(refundId, merchantId, paymentId));

        mockMvc.perform(post("/api/v1/refunds")
                        .with(jwt().jwt(token -> token
                                .subject(userId.toString())
                                .claim("merchant_id", merchantId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentId": "%s",
                                  "amount": 50.00,
                                  "reason": "Customer returned one item"
                                }
                                """.formatted(paymentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(refundId.toString()))
                .andExpect(jsonPath("$.merchantId").value(merchantId.toString()))
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void shouldListMerchantRefunds() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();

        when(refundService.listRefunds(merchantId))
                .thenReturn(List.of(refundResponse(refundId, merchantId, paymentId)));

        mockMvc.perform(get("/api/v1/refunds")
                        .with(jwt().jwt(token -> token
                                .subject(userId.toString())
                                .claim("merchant_id", merchantId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(refundId.toString()))
                .andExpect(jsonPath("$[0].merchantId").value(merchantId.toString()));
    }

    @Test
    void shouldRequireAuthenticationForRefunds() throws Exception {
        mockMvc.perform(get("/api/v1/refunds"))
                .andExpect(status().isUnauthorized());
    }

    private RefundResponse refundResponse(UUID refundId, UUID merchantId, UUID paymentId) {
        return new RefundResponse(
                refundId,
                merchantId,
                paymentId,
                new BigDecimal("50.00"),
                "ZAR",
                "SUCCEEDED",
                "SIM-REF-123",
                "Customer returned one item");
    }
}
