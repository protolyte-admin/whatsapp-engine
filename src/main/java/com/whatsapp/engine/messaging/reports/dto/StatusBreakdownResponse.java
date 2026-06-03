package com.whatsapp.engine.messaging.reports.dto;

import com.whatsapp.engine.messaging.MessageStatus;

public record StatusBreakdownResponse(
        MessageStatus status,
        long count
) {
}
