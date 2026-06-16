package com.whatsapp.engine.messaging.reports;

public record SummaryMetrics(
        long totalMessages,
        long totalSent,
        long totalDelivered,
        long totalRead,
        long totalFailed,
        long messagesSentToday,
        long messagesDeliveredToday,
        long messagesReadToday
) {
}