package za.co.safintech.payments.customer.controller;

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
import za.co.safintech.payments.customer.dto.CreateCustomerRequest;
import za.co.safintech.payments.customer.dto.CustomerResponse;
import za.co.safintech.payments.customer.service.CustomerService;

@RestController
@Profile("!local")
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CustomerResponse createCustomer(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateCustomerRequest request) {
        return customerService.createCustomer(JwtMerchantContext.merchantId(jwt), request);
    }

    @GetMapping
    List<CustomerResponse> listCustomers(@AuthenticationPrincipal Jwt jwt) {
        return customerService.listCustomers(JwtMerchantContext.merchantId(jwt));
    }

    @GetMapping("/{customerId}")
    CustomerResponse getCustomer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID customerId) {
        return customerService.getCustomer(JwtMerchantContext.merchantId(jwt), customerId);
    }
}
