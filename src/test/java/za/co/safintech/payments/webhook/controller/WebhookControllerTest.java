package za.co.safintech.payments.webhook.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import za.co.safintech.payments.common.api.GlobalExceptionHandler;
import za.co.safintech.payments.common.config.SecurityConfig;
import za.co.safintech.payments.webhook.dto.SimulatedPaymentWebhookRequest;
import za.co.safintech.payments.webhook.dto.WebhookEventResponse;
import za.co.safintech.payments.webhook.service.WebhookService;

@WebMvcTest(WebhookController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebhookService webhookService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldAcceptSimulatedWebhookWithoutJwtWhenSecretIsProvided() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        when(webhookService.processPaymentWebhook(eq("test-secret"), any(SimulatedPaymentWebhookRequest.class)))
                .thenReturn(new WebhookEventResponse(eventId, "evt-1", "PROCESSED", paymentId, null));

        mockMvc.perform(post("/api/v1/webhooks/simulated/payments")
                        .header("X-Simulated-Webhook-Secret", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerEventId": "evt-1",
                                  "eventType": "payment.status_changed",
                                  "providerReference": "SIM-PAY-123",
                                  "paymentStatus": "SUCCEEDED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.processingStatus").value("PROCESSED"))
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()));
    }
}
