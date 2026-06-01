package com.whatsapp.engine.campaigns.dto;

import com.whatsapp.engine.campaigns.CampaignStatus;
import com.whatsapp.engine.messaging.MessageType;
import java.time.Instant;
import java.util.UUID;

public record CampaignResponse(
        UUID id,
        UUID organizationId,
        String name,
        MessageType messageType,
        CampaignStatus status,
        Instant scheduledAt,
        Instant launchedAt,
        Instant completedAt,
        CampaignAnalytics analytics
) {

    public record CampaignAnalytics(
            int totalRecipients,
            int sentCount,
            int failedCount,
            int pendingCount
    ) {
    }
}
