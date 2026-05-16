package za.co.safintech.payments.invoice.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import za.co.safintech.payments.common.security.JwtMerchantContext;
import za.co.safintech.payments.invoice.dto.CreateInvoiceRequest;
import za.co.safintech.payments.invoice.dto.InvoiceResponse;
import za.co.safintech.payments.invoice.service.InvoiceService;

@RestController
@Profile("!local")
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    InvoiceResponse createInvoice(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateInvoiceRequest request) {
        return invoiceService.createInvoice(JwtMerchantContext.merchantId(jwt), request);
    }

    @GetMapping
    List<InvoiceResponse> listInvoices(@AuthenticationPrincipal Jwt jwt) {
        return invoiceService.listInvoices(JwtMerchantContext.merchantId(jwt));
    }

    @GetMapping("/{invoiceId}")
    InvoiceResponse getInvoice(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invoiceId) {
        return invoiceService.getInvoice(JwtMerchantContext.merchantId(jwt), invoiceId);
    }

    @PostMapping("/{invoiceId}/cancel")
    InvoiceResponse cancelInvoice(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invoiceId) {
        return invoiceService.cancelInvoice(JwtMerchantContext.merchantId(jwt), invoiceId);
    }
}
