package za.co.safintech.payments.customer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import za.co.safintech.payments.customer.dto.CreateCustomerRequest;
import za.co.safintech.payments.customer.dto.CustomerResponse;
import za.co.safintech.payments.customer.service.CustomerService;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldCreateCustomerForAuthenticatedMerchant() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(customerService.createCustomer(eq(merchantId), any(CreateCustomerRequest.class)))
                .thenReturn(new CustomerResponse(customerId, merchantId, "Lerato Khumalo", "lerato@example.com", "+27821234567"));

        mockMvc.perform(post("/api/v1/customers")
                        .with(jwt().jwt(token -> token.claim("merchant_id", merchantId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Lerato Khumalo",
                                  "email": "lerato@example.com",
                                  "phoneNumber": "+27821234567"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.merchantId").value(merchantId.toString()))
                .andExpect(jsonPath("$.phoneNumber").value("+27821234567"));
    }

    @Test
    void shouldRejectInvalidSouthAfricanPhoneNumber() throws Exception {
        UUID merchantId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/customers")
                        .with(jwt().jwt(token -> token.claim("merchant_id", merchantId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Lerato Khumalo",
                                  "email": "lerato@example.com",
                                  "phoneNumber": "0821234567"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldListOnlyAuthenticatedMerchantCustomers() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(customerService.listCustomers(merchantId))
                .thenReturn(List.of(new CustomerResponse(customerId, merchantId, "Lerato Khumalo", null, "+27821234567")));

        mockMvc.perform(get("/api/v1/customers")
                        .with(jwt().jwt(token -> token.claim("merchant_id", merchantId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchantId").value(merchantId.toString()))
                .andExpect(jsonPath("$[0].id").value(customerId.toString()));
    }

    @Test
    void shouldRequireAuthenticationForCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectTokenWithoutMerchantContext() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                        .with(jwt()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN_CLAIMS"));
    }
}
