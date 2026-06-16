package com.whatsapp.engine.messaging.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.whatsapp.engine.auth.User;
import com.whatsapp.engine.common.exception.ApplicationException;
import com.whatsapp.engine.contacts.Contact;
import com.whatsapp.engine.contacts.repository.ContactRepository;
import com.whatsapp.engine.messaging.Message;
import com.whatsapp.engine.messaging.MessageStatus;
import com.whatsapp.engine.messaging.MessageType;
import com.whatsapp.engine.messaging.client.MetaWhatsAppClient;
import com.whatsapp.engine.messaging.client.MetaWhatsAppSendResponse;
import com.whatsapp.engine.messaging.dto.*;
import com.whatsapp.engine.messaging.repository.MessageRepository;
import com.whatsapp.engine.organization.Organization;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class MessagingService {

    private final MessageRepository messageRepository;
    private final ContactRepository contactRepository;
    private final MetaWhatsAppClient metaWhatsAppClient;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter = RateLimiter.create(3.0);



    private final ExecutorService executorService =
            Executors.newFixedThreadPool(20);


    public MessagingService(
            MessageRepository messageRepository, ContactRepository contactRepository,
            MetaWhatsAppClient metaWhatsAppClient,
            ObjectMapper objectMapper
    ) {
        this.messageRepository = messageRepository;
        this.contactRepository = contactRepository;
        this.metaWhatsAppClient = metaWhatsAppClient;
        this.objectMapper = objectMapper;
    }

    public MessageResponse sendTextMessage(SendTextMessageRequest request, User user) {
        Organization organization = user.getOrganization();
        validateWhatsAppConfiguration(organization);

        Message message = new Message();
        message.setOrganization(organization);
        message.setCreatedBy(user);
        message.setRecipientPhoneNumber(request.to());
        message.setMessageType(MessageType.TEXT);
        message.setStatus(MessageStatus.ACCEPTED);
        message.setTextBody(request.body());
        Message savedMessage = messageRepository.save(message);

        try {
            MetaWhatsAppSendResponse metaResponse = metaWhatsAppClient.sendTextMessage(
                    organization.getWhatsappPhoneNumberId(),
                    organization.getWhatsappAccessToken(),
                    request.to(),
                    request.body()
            );
            markSent(savedMessage, metaResponse);
            log.info(
                    "Text message sent. messageId={}, organizationId={}, metaMessageId={}",
                    savedMessage.getId(),
                    organization.getId(),
                    metaResponse.messageId()
            );
            return toResponse(savedMessage);
        } catch (RuntimeException exception) {
            markFailed(savedMessage, exception);
            throw exception;
        }
    }

    public MessageResponse sendTemplateMessage(SendTemplateMessageRequest request, User user) {
        Organization organization = user.getOrganization();
        validateWhatsAppConfiguration(organization);

        List<String> bodyParameters = request.bodyParameters() == null ? List.of() : request.bodyParameters();
        Message message = new Message();
        message.setOrganization(organization);
        message.setCreatedBy(user);
        message.setRecipientPhoneNumber(request.to());
        message.setMessageType(MessageType.TEMPLATE);
        message.setStatus(MessageStatus.ACCEPTED);
        message.setTemplateName(request.templateName());
        message.setTextBody(request.templateName());
        message.setTemplateLanguage(request.languageCode());
        message.setTemplateParameters(toJson(bodyParameters));
        Message savedMessage = messageRepository.save(message);

        try {
            rateLimiter.acquire();
            MetaWhatsAppSendResponse metaResponse = metaWhatsAppClient.sendTemplateMessage(
                    organization.getWhatsappPhoneNumberId(),
                    organization.getWhatsappAccessToken(),
                    request.to(),
                    request.templateName(),
                    request.languageCode(),
                    bodyParameters
            );
            markSent(savedMessage, metaResponse);
            log.info(
                    "Template message sent. messageId={}, organizationId={}, template={}, metaMessageId={}",
                    savedMessage.getId(),
                    organization.getId(),
                    request.templateName(),
                    metaResponse.messageId()
            );
            return toResponse(savedMessage);
        } catch (RuntimeException exception) {
            markFailed(savedMessage, exception);
            throw exception;
        }
    }

    private void validateWhatsAppConfiguration(Organization organization) {
        if (!StringUtils.hasText(organization.getWhatsappPhoneNumberId())
                || !StringUtils.hasText(organization.getWhatsappAccessToken())) {
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "WHATSAPP_NOT_CONFIGURED",
                    "WhatsApp Cloud API credentials are not configured for this organization"
            );
        }
    }

    private void markSent(Message message, MetaWhatsAppSendResponse metaResponse) {
        message.setMetaMessageId(metaResponse.messageId());
        message.setMetaResponse(metaResponse.rawResponse());
        message.setStatus(MessageStatus.ACCEPTED);
        message.setSentAt(Instant.now());
        messageRepository.save(message);
    }

    private void markFailed(Message message, RuntimeException exception) {
        message.setStatus(MessageStatus.FAILED);
        message.setFailureReason(exception.getMessage());
        message.setFailedAt(Instant.now());
        messageRepository.save(message);
        log.warn("Message send failed. messageId={}, reason={}", message.getId(), exception.getMessage());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "INVALID_TEMPLATE_PARAMETERS", "Invalid template parameters");
        }
    }

    private MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getOrganization().getId(),
                message.getRecipientPhoneNumber(),
                message.getMessageType(),
                message.getStatus(),
                message.getMetaMessageId(),
                message.getSentAt()
        );
    }

    public List<MessageResponse> sendBulkTextMessage(@Valid SendBulkTextMessageRequest request, User user) {
        Organization organization = user.getOrganization();
        validateWhatsAppConfiguration(organization);

        Pageable pageable = PageRequest.of(0, 1000);

        Page<Contact> contactsPage =
                contactRepository.findByOrganizationId(
                        organization.getId(),
                        pageable
                );

        List<Contact> contacts = contactsPage.getContent();

        List<MessageResponse> responses = new ArrayList<>();
        for (Contact contact : contacts) {
            Message message = new Message();
            message.setOrganization(organization);
            message.setCreatedBy(user);
            message.setContact(contact);
            message.setRecipientPhoneNumber(contact.getPhoneNumber());
            message.setMessageType(MessageType.TEXT);
            message.setStatus(MessageStatus.ACCEPTED);
            message.setTextBody(request.body());
            Message savedMessage = messageRepository.save(message);

            try {
                MetaWhatsAppSendResponse metaResponse = metaWhatsAppClient.sendTextMessage(
                        organization.getWhatsappPhoneNumberId(),
                        organization.getWhatsappAccessToken(),
                        contact.getPhoneNumber(),
                        request.body()
                );
                markSent(savedMessage, metaResponse);
                log.info(
                        "Bulk Text message sent. messageId={}, organizationId={}, metaMessageId={}",
                        savedMessage.getId(),
                        organization.getId(),
                        metaResponse.messageId()
                );
                responses.add(toResponse(savedMessage));
            } catch (RuntimeException exception) {
                markFailed(savedMessage, exception);
                throw exception;
            }
        }
        return responses;
    }

    public List<MessageResponse> sendBulkTemplateMessage(
            @Valid SendBulkTemplateMessageRequest request,
            User user, int pageNumber, int pageSize) {

        Organization organization = user.getOrganization();
        validateWhatsAppConfiguration(organization);

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        List<Contact> contacts =
                contactRepository.findByOrganizationId(
                                organization.getId(),
                                pageable)
                        .getContent();

        List<CompletableFuture<MessageResponse>> futures =
                contacts.stream()
                        .map(contact ->
                                CompletableFuture.supplyAsync(
                                        () -> processContact(
                                                contact,
                                                request,
                                                user,
                                                organization),
                                        executorService))
                        .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private MessageResponse processContact(
            Contact contact,
            SendBulkTemplateMessageRequest request,
            User user,
            Organization organization) {

        List<String> bodyParameters =
                request.bodyParameters() == null
                        ? List.of()
                        : request.bodyParameters();

        Message message = new Message();
        message.setOrganization(organization);
        message.setCreatedBy(user);
        message.setContact(contact);
        message.setRecipientPhoneNumber(contact.getPhoneNumber());
        message.setMessageType(MessageType.TEMPLATE);
        message.setStatus(MessageStatus.ACCEPTED);
        message.setTemplateName(request.templateName());
        message.setTemplateLanguage(request.languageCode());
        message.setTemplateParameters(toJson(bodyParameters));
        message.setTemplateName(request.templateName());
        Message savedMessage = messageRepository.save(message);

        try {
            MetaWhatsAppSendResponse response =
                    metaWhatsAppClient.sendTemplateMessage(
                            organization.getWhatsappPhoneNumberId(),
                            organization.getWhatsappAccessToken(),
                            contact.getPhoneNumber(),
                            request.templateName(),
                            request.languageCode(),
                            bodyParameters);

            markSent(savedMessage, response);
            log.info(
                    "Bulk Text message sent. messageId={}, organizationId={}, metaMessageId={}, To Contact Number={}",
                    savedMessage.getId(),
                    organization.getId(),
                    response.messageId(),
                    contact.getPhoneNumber()
            );
            return toResponse(savedMessage);

        } catch (Exception ex) {
            markFailed(savedMessage, (RuntimeException) ex);

            return toResponse(savedMessage);
        }
    }

    public List<ConversationResponse> getConversations(User user) {

        return messageRepository
                .findLatestConversationMessages(
                        user.getOrganization().getId())
                .stream()
                .map(message -> new ConversationResponse(
                        message.getRecipientPhoneNumber(),
                        message.getTextBody(),
                        message.getCreatedAt(),
                        0L
                ))
                .toList();
    }

    public List<ConversationMessageResponse> getMessagesByPhoneNumber(
            String phoneNumber,
            User user) {

        return messageRepository
                .findAllByRecipientPhoneNumberAndOrganizationIdOrderBySentAtAsc(
                        phoneNumber,
                        user.getOrganization().getId())
                .stream()
                .map(this::toConversationMessageResponse)
                .toList();
    }

    private ConversationMessageResponse toConversationMessageResponse(Message message) {

        return new ConversationMessageResponse(
                message.getId(),
                message.getOrganization().getId(),
                message.getRecipientPhoneNumber(),
                message.getTextBody(),
                message.getMessageType(),
                message.getStatus(),
                message.getMetaMessageId(),
                message.getSentAt(),
                message.getDirection().toString()
        );
    }
}
