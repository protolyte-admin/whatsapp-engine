package com.whatsapp.engine.contacts.dto;

import java.time.Instant;
import java.util.UUID;

public record ContactResponse(
        UUID id,
        UUID organizationId,
        String name,
        String phoneNumber,
        String email,
        String notes,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
