package com.whatsapp.engine.messaging.client;

public record MetaWhatsAppSendResponse(
        String messageId,
        String rawResponse
) {
}
