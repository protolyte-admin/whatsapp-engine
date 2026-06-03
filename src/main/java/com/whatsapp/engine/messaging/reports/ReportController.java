package com.whatsapp.engine.messaging.reports;

import com.whatsapp.engine.auth.User;
import com.whatsapp.engine.common.api.ApiResponse;
import com.whatsapp.engine.messaging.MessageStatus;
import com.whatsapp.engine.messaging.reports.dto.DailyTrendResponse;
import com.whatsapp.engine.messaging.reports.dto.MessageReportFilter;
import com.whatsapp.engine.messaging.reports.dto.MessageReportResponse;
import com.whatsapp.engine.messaging.reports.dto.MessageSummaryResponse;
import com.whatsapp.engine.messaging.reports.dto.StatusBreakdownResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('ORG_ADMIN', 'AGENT')")
@Tag(name = "Message Reports", description = "WhatsApp message analytics and report APIs")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard message delivery summary")
    public ResponseEntity<ApiResponse<MessageSummaryResponse>> getSummary(
            @AuthenticationPrincipal User user,
            ReportQuery query
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Message summary fetched",
                reportService.getSummary(user.getOrganization().getId(), query.toFilter())
        ));
    }

    @GetMapping("/messages")
    @Operation(summary = "Get paginated message report")
    public ResponseEntity<ApiResponse<Page<MessageReportResponse>>> getMessages(
            @AuthenticationPrincipal User user,
            ReportQuery query,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Message report fetched",
                reportService.getMessages(user.getOrganization().getId(), query.toFilter(), pageable)
        ));
    }

    @GetMapping("/messages/{id}")
    @Operation(summary = "Get a single message report row")
    public ResponseEntity<ApiResponse<MessageReportResponse>> getMessage(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Message report item fetched",
                reportService.getMessage(user.getOrganization().getId(), id)
        ));
    }

    @GetMapping("/status-breakdown")
    @Operation(summary = "Get message count grouped by current status")
    public ResponseEntity<ApiResponse<List<StatusBreakdownResponse>>> getStatusBreakdown(
            @AuthenticationPrincipal User user,
            ReportQuery query
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Status breakdown fetched",
                reportService.getStatusBreakdown(user.getOrganization().getId(), query.toFilter())
        ));
    }

    @GetMapping("/daily-trend")
    @Operation(summary = "Get daily outbound message trend")
    public ResponseEntity<ApiResponse<List<DailyTrendResponse>>> getDailyTrend(
            @AuthenticationPrincipal User user,
            ReportQuery query
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Daily trend fetched",
                reportService.getDailyTrend(user.getOrganization().getId(), query.toFilter())
        ));
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export message report as CSV")
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal User user,
            ReportQuery query
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=message-report.csv")
                .body(reportService.exportCsv(user.getOrganization(), query.toFilter()));
    }

    @GetMapping("/export/excel")
    @Operation(summary = "Export message report as Excel")
    public ResponseEntity<byte[]> exportExcel(
            @AuthenticationPrincipal User user,
            ReportQuery query
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=message-report.xlsx")
                .body(reportService.exportExcel(user.getOrganization(), query.toFilter()));
    }

    public record ReportQuery(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @RequestParam(required = false)
            Instant dateFrom,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @RequestParam(required = false)
            Instant dateTo,
            @RequestParam(required = false)
            String templateName,
            @RequestParam(required = false)
            UUID campaignId,
            @RequestParam(required = false)
            UUID contactId,
            @RequestParam(required = false)
            MessageStatus status
    ) {
        MessageReportFilter toFilter() {
            return new MessageReportFilter(dateFrom, dateTo, templateName, campaignId, contactId, status);
        }
    }
}
