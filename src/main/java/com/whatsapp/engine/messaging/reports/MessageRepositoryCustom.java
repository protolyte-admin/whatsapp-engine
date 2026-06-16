package com.whatsapp.engine.messaging.reports;

import com.whatsapp.engine.messaging.reports.dto.MessageReportFilter;

import java.util.UUID;

public interface MessageRepositoryCustom {
    SummaryMetrics getDashboardSummary(
            UUID organizationId,
            MessageReportFilter filter
    );
}