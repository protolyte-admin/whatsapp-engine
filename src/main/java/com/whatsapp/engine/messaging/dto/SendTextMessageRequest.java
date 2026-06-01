package com.whatsapp.engine.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendTextMessageRequest(
        @NotBlank(message = "Recipient phone number is required")
        @Pattern(regexp = "^[1-9][0-9]{7,14}$", message = "Recipient phone number must be in E.164 format without +")
        String to,

        @NotBlank(message = "Message body is required")
        @Size(max = 4096, message = "Message body must be at most 4096 characters")
        String body
) {
}
