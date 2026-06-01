package com.whatsapp.engine.campaigns.dto;

import com.whatsapp.engine.messaging.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateCampaignRequest(
        @NotBlank(message = "Campaign name is required")
        @Size(max = 150, message = "Campaign name must be at most 150 characters")
        String name,

        @NotNull(message = "Message type is required")
        MessageType messageType,

        @Size(max = 4096, message = "Text body must be at most 4096 characters")
        String textBody,

        @Size(max = 150, message = "Template name must be at most 150 characters")
        String templateName,

        @Size(max = 20, message = "Template language must be at most 20 characters")
        String templateLanguage,

        @Size(max = 20, message = "A template message can include at most 20 body parameters")
        List<@Size(max = 1024, message = "Template parameter must be at most 1024 characters") String> templateParameters,

        Instant scheduledAt,

        @NotEmpty(message = "At least one contact is required")
        @Size(max = 5000, message = "A campaign can target at most 5000 contacts")
        List<@NotNull(message = "Contact id is required") UUID> contactIds
) {
}
