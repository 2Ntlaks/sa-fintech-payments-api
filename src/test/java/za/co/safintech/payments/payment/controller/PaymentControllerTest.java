package za.co.safintech.payments.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

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
import za.co.safintech.payments.payment.dto.CreatePaymentRequest;
import za.co.safintech.payments.payment.dto.PaymentResponse;
import za.co.safintech.payments.payment.dto.UpdatePaymentStatusRequest;
import za.co.safintech.payments.payment.service.PaymentService;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldCreatePaymentForAuthenticatedMerchant() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        when(paymentService.createPaymentAttempt(eq(merchantId), eq(userId), any(CreatePaymentRequest.class), isNull()))
                .thenReturn(paymentResponse(paymentId, merchantId, invoiceId, "CREATED"));

        mockMvc.perform(post("/api/v1/payments")
                        .with(jwt().jwt(token -> token
                                .subject(userId.toString())
                                .claim("merchant_id", merchantId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "invoiceId": "%s",
                                  "paymentMethod": "CARD_SIMULATED"
                                }
                                """.formatted(invoiceId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.merchantId").value(merchantId.toString()))
                .andExpect(jsonPath("$.invoiceId").value(invoiceId.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void shouldPassIdempotencyKeyWhenCreatingPayment() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        when(paymentService.createPaymentAttempt(eq(merchantId), eq(userId), any(CreatePaymentRequest.class), eq("retry-key-1")))
                .thenReturn(paymentResponse(paymentId, merchantId, invoiceId, "CREATED"));

        mockMvc.perform(post("/api/v1/payments")
                        .with(jwt().jwt(token -> token
                                .subject(userId.toString())
                                .claim("merchant_id", merchantId.toString())))
                        .header("Idempotency-Key", "retry-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "invoiceId": "%s",
                                  "paymentMethod": "CARD_SIMULATED"
                                }
                                """.formatted(invoiceId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentId.toString()));
    }

    @Test
    void shouldListMerchantPayments() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        when(paymentService.listPayments(merchantId))
                .thenReturn(List.of(paymentResponse(paymentId, merchantId, invoiceId, "PROCESSING")));

        mockMvc.perform(get("/api/v1/payments")
                        .with(jwt().jwt(token -> token
                                .subject(userId.toString())
                                .claim("merchant_id", merchantId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(paymentId.toString()))
                .andExpect(jsonPath("$[0].merchantId").value(merchantId.toString()));
    }

    @Test
    void shouldUpdatePaymentStatusForAuthenticatedMerchant() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        when(paymentService.updatePaymentStatus(eq(merchantId), eq(userId), eq(paymentId), any(UpdatePaymentStatusRequest.class)))
                .thenReturn(paymentResponse(paymentId, merchantId, invoiceId, "SUCCEEDED"));

        mockMvc.perform(post("/api/v1/payments/{paymentId}/status", paymentId)
                        .with(jwt().jwt(token -> token
                                .subject(userId.toString())
                                .claim("merchant_id", merchantId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SUCCEEDED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void shouldRequireAuthenticationForPayments() throws Exception {
        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectTokenWithInvalidMerchantContext() throws Exception {
        mockMvc.perform(get("/api/v1/payments")
                        .with(jwt().jwt(token -> token.claim("merchant_id", "not-a-uuid"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN_CLAIMS"));
    }

    private PaymentResponse paymentResponse(UUID paymentId, UUID merchantId, UUID invoiceId, String status) {
        return new PaymentResponse(
                paymentId,
                merchantId,
                invoiceId,
                new BigDecimal("125.50"),
                new BigDecimal("125.50"),
                new BigDecimal("4.64"),
                new BigDecimal("120.86"),
                "ZAR",
                "CARD_SIMULATED",
                status,
                "SIM-PAY-123",
                null);
    }
}
