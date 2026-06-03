package com.whatsapp.engine.messaging.reports.dto;

import com.whatsapp.engine.messaging.MessageStatus;
import com.whatsapp.engine.messaging.MessageType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageReportResponse(
        UUID id,
        String whatsappMessageId,
        UUID contactId,
        String phoneNumber,
        String toPhoneNumber,
        String templateName,
        UUID campaignId,
        String messageBody,
        MessageType messageType,
        MessageStatus currentStatus,
        Instant sentAt,
        Instant deliveredAt,
        Instant readAt,
        Instant failedAt,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        List<MessageStatusHistoryResponse> statusHistory
) {
}
