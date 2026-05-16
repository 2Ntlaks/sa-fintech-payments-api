package za.co.safintech.payments.payment.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import za.co.safintech.payments.common.security.JwtMerchantContext;
import za.co.safintech.payments.payment.dto.CreatePaymentRequest;
import za.co.safintech.payments.payment.dto.PaymentResponse;
import za.co.safintech.payments.payment.dto.UpdatePaymentStatusRequest;
import za.co.safintech.payments.payment.service.PaymentService;

@RestController
@Profile("!local")
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PaymentResponse createPayment(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPaymentAttempt(
                JwtMerchantContext.merchantId(jwt),
                JwtMerchantContext.merchantUserId(jwt),
                request,
                idempotencyKey);
    }

    @GetMapping
    List<PaymentResponse> listPayments(@AuthenticationPrincipal Jwt jwt) {
        return paymentService.listPayments(JwtMerchantContext.merchantId(jwt));
    }

    @GetMapping("/{paymentId}")
    PaymentResponse getPayment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
        return paymentService.getPayment(JwtMerchantContext.merchantId(jwt), paymentId);
    }

    @PostMapping("/{paymentId}/status")
    PaymentResponse updatePaymentStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId,
            @Valid @RequestBody UpdatePaymentStatusRequest request) {
        return paymentService.updatePaymentStatus(
                JwtMerchantContext.merchantId(jwt),
                JwtMerchantContext.merchantUserId(jwt),
                paymentId,
                request);
    }
}
