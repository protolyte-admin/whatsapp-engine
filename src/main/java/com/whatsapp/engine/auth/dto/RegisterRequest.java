package com.whatsapp.engine.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Organization name is required")
        @Size(max = 150, message = "Organization name must be at most 150 characters")
        String organizationName,

        @NotBlank(message = "Organization slug is required")
        @Size(min = 3, max = 80, message = "Organization slug must be between 3 and 80 characters")
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Organization slug must use lowercase letters, numbers, and hyphens"
        )
        String organizationSlug,

        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must be at most 120 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 180, message = "Email must be at most 180 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must include uppercase, lowercase, and numeric characters"
        )
        String password
) {
}
