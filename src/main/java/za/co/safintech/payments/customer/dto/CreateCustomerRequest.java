package za.co.safintech.payments.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        @NotBlank @Size(max = 160) String fullName,
        @Email @Size(max = 320) String email,
        @NotBlank @Pattern(regexp = "^\\+27[0-9]{9}$", message = "must be a South African +27 number") String phoneNumber) {
}
