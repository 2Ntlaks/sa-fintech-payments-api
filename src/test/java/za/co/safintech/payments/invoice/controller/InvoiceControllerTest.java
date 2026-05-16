package za.co.safintech.payments.invoice.controller;

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
import za.co.safintech.payments.invoice.dto.CreateInvoiceRequest;
import za.co.safintech.payments.invoice.dto.InvoiceResponse;
import za.co.safintech.payments.invoice.service.InvoiceService;

@WebMvcTest(InvoiceController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldCreateInvoiceForAuthenticatedMerchant() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.createInvoice(eq(merchantId), any(CreateInvoiceRequest.class)))
                .thenReturn(new InvoiceResponse(invoiceId, merchantId, customerId, "INV-ABC12345", "Lesson", new BigDecimal("250.00"), "ZAR", "ISSUED", null));

        mockMvc.perform(post("/api/v1/invoices")
                        .with(jwt().jwt(token -> token.claim("merchant_id", merchantId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "description": "Lesson",
                                  "amount": 250.00
                                }
                                """.formatted(customerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.merchantId").value(merchantId.toString()))
                .andExpect(jsonPath("$.currency").value("ZAR"))
                .andExpect(jsonPath("$.status").value("ISSUED"));
    }

    @Test
    void shouldListMerchantInvoices() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.listInvoices(merchantId))
                .thenReturn(List.of(new InvoiceResponse(invoiceId, merchantId, customerId, "INV-ABC12345", "Lesson", new BigDecimal("250.00"), "ZAR", "ISSUED", null)));

        mockMvc.perform(get("/api/v1/invoices")
                        .with(jwt().jwt(token -> token.claim("merchant_id", merchantId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(invoiceId.toString()))
                .andExpect(jsonPath("$[0].merchantId").value(merchantId.toString()));
    }

    @Test
    void shouldCancelInvoiceForAuthenticatedMerchant() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.cancelInvoice(merchantId, invoiceId))
                .thenReturn(new InvoiceResponse(invoiceId, merchantId, customerId, "INV-ABC12345", "Lesson", new BigDecimal("250.00"), "ZAR", "CANCELLED", null));

        mockMvc.perform(post("/api/v1/invoices/{invoiceId}/cancel", invoiceId)
                        .with(jwt().jwt(token -> token.claim("merchant_id", merchantId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldRequireAuthenticationForInvoices() throws Exception {
        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectTokenWithInvalidMerchantContext() throws Exception {
        mockMvc.perform(get("/api/v1/invoices")
                        .with(jwt().jwt(token -> token.claim("merchant_id", "not-a-uuid"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN_CLAIMS"));
    }
}
