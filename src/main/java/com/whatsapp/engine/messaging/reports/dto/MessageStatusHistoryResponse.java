package com.whatsapp.engine.messaging.reports.dto;

import com.whatsapp.engine.messaging.MessageStatus;
import java.time.Instant;
import java.util.UUID;

public record MessageStatusHistoryResponse(
        UUID id,
        UUID messageId,
        String toPhoneNumber,
        MessageStatus previousStatus,
        MessageStatus newStatus,
        Instant statusTimestamp,
        Instant createdAt
) {
}
