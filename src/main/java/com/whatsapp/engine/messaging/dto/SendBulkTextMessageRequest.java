package com.whatsapp.engine.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendBulkTextMessageRequest(
        @NotBlank(message = "Message body is required")
        @Size(max = 4096, message = "Message body must be at most 4096 characters")
        String body
) {
}
