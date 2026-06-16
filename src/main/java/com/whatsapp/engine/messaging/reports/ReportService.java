package com.whatsapp.engine.messaging.reports;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.engine.common.exception.ApplicationException;
import com.whatsapp.engine.messaging.Message;
import com.whatsapp.engine.messaging.MessageReport;
import com.whatsapp.engine.messaging.MessageStatus;
import com.whatsapp.engine.messaging.MessageStatusHistory;
import com.whatsapp.engine.messaging.reports.dto.DailyTrendResponse;
import com.whatsapp.engine.messaging.reports.dto.MessageReportFilter;
import com.whatsapp.engine.messaging.reports.dto.MessageReportResponse;
import com.whatsapp.engine.messaging.reports.dto.MessageStatusHistoryResponse;
import com.whatsapp.engine.messaging.reports.dto.MessageSummaryResponse;
import com.whatsapp.engine.messaging.reports.dto.StatusBreakdownResponse;
import com.whatsapp.engine.messaging.repository.MessageReportRepository;
import com.whatsapp.engine.messaging.repository.MessageRepository;
import com.whatsapp.engine.messaging.repository.MessageStatusHistoryRepository;
import com.whatsapp.engine.organization.Organization;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final MessageReportRepository messageReportRepository;
    private final MessageRepository messageRepository;
    private final MessageStatusHistoryRepository messageStatusHistoryRepository;
    private final ObjectMapper objectMapper;

    public ReportService(
            MessageReportRepository messageReportRepository,
            MessageRepository messageRepository,
            MessageStatusHistoryRepository messageStatusHistoryRepository,
            ObjectMapper objectMapper
    ) {
        this.messageReportRepository = messageReportRepository;
        this.messageRepository = messageRepository;
        this.messageStatusHistoryRepository = messageStatusHistoryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public MessageSummaryResponse getSummary(
            UUID organizationId,
            MessageReportFilter filter
    ) {

        SummaryMetrics summary =
                messageRepository.getDashboardSummary(
                        organizationId,
                        filter
                );

        return new MessageSummaryResponse(
                summary.totalSent(),
                summary.totalDelivered(),
                summary.totalRead(),
                summary.totalFailed(),

                rate(summary.totalDelivered(),
                        summary.totalSent()),

                rate(summary.totalRead(),
                        summary.totalDelivered()),

                rate(summary.totalFailed(),
                        summary.totalMessages()),

                summary.messagesSentToday(),
                summary.messagesDeliveredToday(),
                summary.messagesReadToday()
        );
    }

    @Transactional(readOnly = true)
    public Page<MessageReportResponse> getMessages(UUID organizationId, MessageReportFilter filter, Pageable pageable) {
        Instant effectiveDateFrom =
                filter.dateFrom() != null
                        ? filter.dateFrom()
                        : Instant.parse("1970-01-01T00:00:00Z");

        Instant effectiveDateTo =
                filter.dateTo() != null
                        ? filter.dateTo()
                        : Instant.now();
        return messageRepository.findReportMessages(
                organizationId,
                effectiveDateFrom,
                effectiveDateTo,
                filter.templateName(),
                filter.campaignId(),
                filter.contactId(),
                filter.status(),
                pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MessageReportResponse getMessage(UUID organizationId, UUID messageId) {
        return messageRepository.findByIdAndOrganizationId(messageId, organizationId)
                .map(message -> toResponse(message, true))
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "MESSAGE_NOT_FOUND", "Message not found"));
    }

    @Transactional(readOnly = true)
    public List<StatusBreakdownResponse> getStatusBreakdown(UUID organizationId, MessageReportFilter filter) {
        return messageRepository.getStatusBreakdown(
                        organizationId,
                        filter.dateFrom(),
                        filter.dateTo(),
                        filter.templateName(),
                        filter.campaignId(),
                        filter.contactId(),
                        filter.status()
                ).stream()
                .map(row -> new StatusBreakdownResponse((MessageStatus) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DailyTrendResponse> getDailyTrend(UUID organizationId, MessageReportFilter filter) {
        return messageRepository.getDailyTrend(
                        organizationId,
                        filter.dateFrom(),
                        filter.dateTo(),
                        filter.templateName(),
                        filter.campaignId(),
                        filter.contactId(),
                        filter.status()
                ).stream()
                .map(row -> new DailyTrendResponse(toLocalDate(row[0]), ((Number) row[1]).longValue()))
                .toList();
    }

    @Transactional
    public byte[] exportCsv(Organization organization, MessageReportFilter filter) {
        List<MessageReportResponse> rows = allRows(organization.getId(), filter);
        saveReportMetadata(organization, "CSV", filter, rows.size());

        StringBuilder csv = new StringBuilder();
        csv.append("id,whatsappMessageId,toPhoneNumber,templateName,campaignId,currentStatus,sentAt,deliveredAt,readAt,failedAt,failureReason\n");
        for (MessageReportResponse row : rows) {
            csv.append(csv(row.id())).append(',')
                    .append(csv(row.whatsappMessageId())).append(',')
                    .append(csv(row.toPhoneNumber())).append(',')
                    .append(csv(row.templateName())).append(',')
                    .append(csv(row.campaignId())).append(',')
                    .append(csv(row.currentStatus())).append(',')
                    .append(csv(row.sentAt())).append(',')
                    .append(csv(row.deliveredAt())).append(',')
                    .append(csv(row.readAt())).append(',')
                    .append(csv(row.failedAt())).append(',')
                    .append(csv(row.failureReason())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public byte[] exportExcel(Organization organization, MessageReportFilter filter) {
        List<MessageReportResponse> rows = allRows(organization.getId(), filter);
        saveReportMetadata(organization, "EXCEL", filter, rows.size());

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("messages");
            List<String> headers = List.of(
                    "ID", "WhatsApp Message ID", "To Phone Number", "Template", "Campaign ID",
                    "Current Status", "Sent At", "Delivered At", "Read At", "Failed At", "Failure Reason"
            );
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }

            for (int i = 0; i < rows.size(); i++) {
                MessageReportResponse source = rows.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(text(source.id()));
                row.createCell(1).setCellValue(text(source.whatsappMessageId()));
                row.createCell(2).setCellValue(text(source.toPhoneNumber()));
                row.createCell(3).setCellValue(text(source.templateName()));
                row.createCell(4).setCellValue(text(source.campaignId()));
                row.createCell(5).setCellValue(text(source.currentStatus()));
                row.createCell(6).setCellValue(text(source.sentAt()));
                row.createCell(7).setCellValue(text(source.deliveredAt()));
                row.createCell(8).setCellValue(text(source.readAt()));
                row.createCell(9).setCellValue(text(source.failedAt()));
                row.createCell(10).setCellValue(text(source.failureReason()));
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new ApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, "EXCEL_EXPORT_FAILED", "Unable to export Excel report");
        }
    }

    private List<MessageReportResponse> allRows(UUID organizationId, MessageReportFilter filter) {
        return messageRepository.findReportMessages(
                        organizationId,
                        filter.dateFrom(),
                        filter.dateTo(),
                        filter.templateName(),
                        filter.campaignId(),
                        filter.contactId(),
                        filter.status(),
                        Pageable.unpaged()
                ).map(message -> toResponse(message, false))
                .toList();
    }

    private void saveReportMetadata(Organization organization, String reportType, MessageReportFilter filter, long totalRows) {
        MessageReport report = new MessageReport();
        report.setOrganization(organization);
        report.setReportType(reportType);
        report.setDateFrom(filter.dateFrom());
        report.setDateTo(filter.dateTo());
        report.setTotalMessages(totalRows);
        report.setFilters(toJson(filter));
        report.setGeneratedAt(Instant.now());
        messageReportRepository.save(report);
    }

    private MessageReportResponse toResponse(Message message) {
        return toResponse(message, false);
    }

    private MessageReportResponse toResponse(Message message, boolean includeHistory) {
        return new MessageReportResponse(
                message.getId(),
                message.getMetaMessageId(),
                message.getContact() == null ? null : message.getContact().getId(),
                message.getRecipientPhoneNumber(),
                message.getRecipientPhoneNumber(),
                message.getTemplateName(),
                message.getCampaign() == null ? null : message.getCampaign().getId(),
                message.getTextBody(),
                message.getMessageType(),
                message.getStatus(),
                message.getSentAt(),
                message.getDeliveredAt(),
                message.getReadAt(),
                message.getFailedAt(),
                message.getFailureReason(),
                message.getCreatedAt(),
                message.getUpdatedAt(),
                includeHistory ? statusHistory(message.getId()) : List.of()
        );
    }

    private List<MessageStatusHistoryResponse> statusHistory(UUID messageId) {
        return messageStatusHistoryRepository.findByMessageIdOrderByStatusTimestampAsc(messageId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private MessageStatusHistoryResponse toHistoryResponse(MessageStatusHistory history) {
        return new MessageStatusHistoryResponse(
                history.getId(),
                history.getMessage().getId(),
                history.getToPhoneNumber(),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getStatusTimestamp(),
                history.getCreatedAt()
        );
    }

    private long number(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) {
            return 0;
        }
        return ((Number) row[index]).longValue();
    }

    private double rate(long numerator, long denominator) {
        if (denominator == 0) {
            return 0;
        }
        return BigDecimal.valueOf(numerator * 100.0 / denominator)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String csv(Object value) {
        String text = text(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
