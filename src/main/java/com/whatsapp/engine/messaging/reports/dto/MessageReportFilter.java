package com.whatsapp.engine.messaging.reports.dto;

import com.whatsapp.engine.messaging.MessageStatus;
import java.time.Instant;
import java.util.UUID;

public record MessageReportFilter(
        Instant dateFrom,
        Instant dateTo,
        String templateName,
        UUID campaignId,
        UUID contactId,
        MessageStatus status
) {
}
