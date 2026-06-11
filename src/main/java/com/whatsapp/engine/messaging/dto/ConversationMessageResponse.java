package com.whatsapp.engine.messaging.dto;

import com.whatsapp.engine.messaging.MessageStatus;
import com.whatsapp.engine.messaging.MessageType;

import java.time.Instant;
import java.util.UUID;


public record ConversationMessageResponse(
        UUID id,
        UUID organizationId,
        String recipientPhoneNumber,
        String content,
        MessageType messageType,
        MessageStatus status,
        String metaMessageId,
        Instant sentAt,
        String direction
) {
}
