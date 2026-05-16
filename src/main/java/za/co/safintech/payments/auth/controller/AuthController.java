package za.co.safintech.payments.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import za.co.safintech.payments.auth.dto.AuthResponse;
import za.co.safintech.payments.auth.dto.LoginRequest;
import za.co.safintech.payments.auth.dto.RegisterMerchantRequest;
import za.co.safintech.payments.auth.service.AuthService;

@RestController
@Profile("!local")
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse register(@Valid @RequestBody RegisterMerchantRequest request) {
        return authService.registerMerchant(request);
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
