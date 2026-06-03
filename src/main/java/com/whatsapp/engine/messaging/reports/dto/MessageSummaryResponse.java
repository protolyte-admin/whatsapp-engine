package com.whatsapp.engine.messaging.reports.dto;

public record MessageSummaryResponse(
        long totalSent,
        long totalDelivered,
        long totalRead,
        long totalFailed,
        double deliveryRate,
        double readRate,
        double failureRate,
        long messagesSentToday,
        long messagesDeliveredToday,
        long messagesReadToday
) {
}
