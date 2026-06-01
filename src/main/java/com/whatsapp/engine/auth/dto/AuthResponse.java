package com.whatsapp.engine.auth.dto;

import com.whatsapp.engine.auth.Role;
import java.util.UUID;

public record AuthResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        UserSummary user,
        OrganizationSummary organization
) {

    public static AuthResponse bearer(
            String accessToken,
            long expiresIn,
            UserSummary user,
            OrganizationSummary organization
    ) {
        return new AuthResponse("Bearer", accessToken, expiresIn, user, organization);
    }

    public record UserSummary(
            UUID id,
            String fullName,
            String email,
            Role role
    ) {
    }

    public record OrganizationSummary(
            UUID id,
            String name,
            String slug
    ) {
    }
}
