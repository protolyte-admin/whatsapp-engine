package com.whatsapp.engine.sse.dto;



import java.time.Instant;
import java.util.UUID;

public record MessageEvent(
        UUID id,
        String phoneNumber,
        String text,
        String direction,
        Instant createdAt
) {
}