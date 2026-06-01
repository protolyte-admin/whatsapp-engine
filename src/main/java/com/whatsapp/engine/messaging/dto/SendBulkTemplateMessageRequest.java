package com.whatsapp.engine.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SendBulkTemplateMessageRequest(

        @NotBlank(message = "Template name is required")
        @Size(max = 150, message = "Template name must be at most 150 characters")
        String templateName,

        @NotBlank(message = "Template language is required")
        @Size(max = 20, message = "Template language must be at most 20 characters")
        String languageCode,

        @Size(max = 20, message = "A template message can include at most 20 body parameters")
        List<
                @NotBlank(message = "Template parameter must not be blank")
                @Size(max = 1024, message = "Template parameter must be at most 1024 characters")
                String
        > bodyParameters
) {
}
