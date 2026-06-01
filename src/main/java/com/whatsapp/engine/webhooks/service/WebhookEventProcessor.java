package com.whatsapp.engine.webhooks.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.engine.common.exception.ApplicationException;
import com.whatsapp.engine.messaging.Message;
import com.whatsapp.engine.messaging.MessageDirection;
import com.whatsapp.engine.messaging.MessageStatus;
import com.whatsapp.engine.messaging.MessageType;
import com.whatsapp.engine.messaging.repository.MessageRepository;
import com.whatsapp.engine.organization.Organization;
import com.whatsapp.engine.organization.repository.OrganizationRepository;
import com.whatsapp.engine.webhooks.WebhookEvent;
import com.whatsapp.engine.webhooks.WebhookEventStatus;
import com.whatsapp.engine.webhooks.repository.WebhookEventRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class WebhookEventProcessor {

    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final OrganizationRepository organizationRepository;
    private final WebhookEventRepository webhookEventRepository;

    public WebhookEventProcessor(
            MessageRepository messageRepository,
            ObjectMapper objectMapper,
            OrganizationRepository organizationRepository,
            WebhookEventRepository webhookEventRepository
    ) {
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
        this.organizationRepository = organizationRepository;
        this.webhookEventRepository = webhookEventRepository;
    }

    @Async
    @Transactional
    public void processMetaWebhookAsync(UUID eventId) {
        WebhookEvent event = webhookEventRepository.findById(eventId)
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "WEBHOOK_EVENT_NOT_FOUND", "Webhook event not found"));

        try {
            JsonNode root = objectMapper.readTree(event.getRawPayload());
            processEntries(root, event);
            event.setStatus(WebhookEventStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            webhookEventRepository.save(event);
            log.info("Meta webhook processed. eventId={}, eventType={}", event.getId(), event.getEventType());
        } catch (Exception exception) {
            event.setStatus(WebhookEventStatus.FAILED);
            event.setErrorMessage(exception.getMessage());
            event.setProcessedAt(Instant.now());
            webhookEventRepository.save(event);
            log.warn("Meta webhook processing failed. eventId={}, reason={}", event.getId(), exception.getMessage());
        }
    }

    private void processEntries(JsonNode root, WebhookEvent event) {
        JsonNode entries = root.path("entry");
        if (!entries.isArray()) {
            return;
        }

        for (JsonNode entry : entries) {
            JsonNode changes = entry.path("changes");
            if (!changes.isArray()) {
                continue;
            }

            for (JsonNode change : changes) {
                JsonNode value = change.path("value");
                String phoneNumberId = value.path("metadata").path("phone_number_id").asText(null);
                Organization organization = resolveOrganization(phoneNumberId).orElse(null);

                event.setPhoneNumberId(phoneNumberId);
                event.setOrganization(organization);

                processStatusUpdates(value, event);
                processIncomingMessages(value, event, organization);
            }
        }
    }

    private void processStatusUpdates(JsonNode value, WebhookEvent event) {
        JsonNode statuses = value.path("statuses");
        if (!statuses.isArray()) {
            return;
        }

        for (JsonNode statusNode : statuses) {
            String metaMessageId = statusNode.path("id").asText(null);
            String status = statusNode.path("status").asText(null);

            event.setMetaMessageId(metaMessageId);
            event.setEventType("message_status_" + status);

            if (!StringUtils.hasText(metaMessageId) || !StringUtils.hasText(status)) {
                continue;
            }

            messageRepository.findByMetaMessageId(metaMessageId)
                    .ifPresent(message -> applyStatus(message, status, statusNode));
        }
    }

    private void processIncomingMessages(JsonNode value, WebhookEvent event, Organization organization) {
        JsonNode messages = value.path("messages");
        if (!messages.isArray()) {
            return;
        }

        for (JsonNode incomingMessage : messages) {
            String metaMessageId = incomingMessage.path("id").asText(null);
            String from = incomingMessage.path("from").asText(null);
            String type = incomingMessage.path("type").asText("unknown");

            event.setMetaMessageId(metaMessageId);
            event.setEventType("incoming_message_" + type);

            if (organization == null || !StringUtils.hasText(metaMessageId)) {
                continue;
            }

            if (messageRepository.findByMetaMessageId(metaMessageId).isPresent()) {
                continue;
            }

            Message message = new Message();
            message.setOrganization(organization);
            message.setRecipientPhoneNumber(from);
            message.setDirection(MessageDirection.INBOUND);
            message.setMessageType(resolveIncomingMessageType(type));
            message.setStatus(MessageStatus.DELIVERED);
            message.setMetaMessageId(metaMessageId);
            message.setTextBody(extractIncomingText(incomingMessage));
            message.setRawWebhookPayload(event.getRawPayload());
            messageRepository.save(message);
        }
    }

    private Optional<Organization> resolveOrganization(String phoneNumberId) {
        if (!StringUtils.hasText(phoneNumberId)) {
            return Optional.empty();
        }
        return organizationRepository.findByWhatsappPhoneNumberId(phoneNumberId);
    }

    private void applyStatus(Message message, String status, JsonNode statusNode) {
        Instant eventTime = parseMetaTimestamp(statusNode.path("timestamp").asText(null));
        switch (status) {
            case "sent" -> {
                message.setStatus(MessageStatus.SENT);
                message.setSentAt(eventTime);
            }
            case "delivered" -> {
                message.setStatus(MessageStatus.DELIVERED);
                message.setDeliveredAt(eventTime);
            }
            case "read" -> {
                message.setStatus(MessageStatus.READ);
                message.setReadAt(eventTime);
            }
            case "failed" -> {
                message.setStatus(MessageStatus.FAILED);
                message.setFailureReason(extractFailureReason(statusNode));
            }
            default -> log.debug("Ignoring unsupported Meta message status={}", status);
        }
        messageRepository.save(message);
    }

    private Instant parseMetaTimestamp(String timestamp) {
        if (!StringUtils.hasText(timestamp)) {
            return Instant.now();
        }
        try {
            return Instant.ofEpochSecond(Long.parseLong(timestamp));
        } catch (NumberFormatException exception) {
            return Instant.now();
        }
    }

    private String extractFailureReason(JsonNode statusNode) {
        JsonNode errors = statusNode.path("errors");
        if (errors.isArray() && errors.size() > 0) {
            JsonNode error = errors.get(0);
            String title = error.path("title").asText("");
            String details = error.path("error_data").path("details").asText("");
            return (title + " " + details).trim();
        }
        return "Meta reported message delivery failure";
    }

    private MessageType resolveIncomingMessageType(String type) {
        if ("text".equals(type)) {
            return MessageType.TEXT;
        }
        return MessageType.TEXT;
    }

    private String extractIncomingText(JsonNode incomingMessage) {
        if ("text".equals(incomingMessage.path("type").asText())) {
            return incomingMessage.path("text").path("body").asText(null);
        }
        return incomingMessage.toString();
    }
}
