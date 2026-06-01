package com.whatsapp.engine.contacts.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must be at most 120 characters")
        String name,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[1-9][0-9]{7,14}$", message = "Phone number must be in E.164 format without +")
        String phoneNumber,

        @Email(message = "Email must be valid")
        @Size(max = 180, message = "Email must be at most 180 characters")
        String email,

        @Size(max = 2000, message = "Notes must be at most 2000 characters")
        String notes
) {
}
