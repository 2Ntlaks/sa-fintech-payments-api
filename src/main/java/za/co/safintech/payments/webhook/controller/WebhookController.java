package za.co.safintech.payments.webhook.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import za.co.safintech.payments.webhook.dto.SimulatedPaymentWebhookRequest;
import za.co.safintech.payments.webhook.dto.WebhookEventResponse;
import za.co.safintech.payments.webhook.service.WebhookService;

@RestController
@Profile("!local")
@RequestMapping("/api/v1/webhooks/simulated")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/payments")
    WebhookEventResponse processPaymentWebhook(
            @RequestHeader(name = "X-Simulated-Webhook-Secret", required = false) String webhookSecret,
            @Valid @RequestBody SimulatedPaymentWebhookRequest request) {
        return webhookService.processPaymentWebhook(webhookSecret, request);
    }
}
