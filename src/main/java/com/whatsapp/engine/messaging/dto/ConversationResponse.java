package com.whatsapp.engine.messaging.dto;

import java.time.Instant;


public record ConversationResponse(
        String phoneNumber,
        String lastMessage,
        Instant lastMessageTime,
        Long unreadCount
) {
}