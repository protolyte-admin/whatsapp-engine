package com.whatsapp.engine.messaging.reports.dto;

import java.time.LocalDate;

public record DailyTrendResponse(
        LocalDate date,
        long count
) {
}
