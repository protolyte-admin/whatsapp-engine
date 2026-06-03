package com.whatsapp.engine.campaigns.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.engine.campaigns.Campaign;
import com.whatsapp.engine.campaigns.CampaignMessage;
import com.whatsapp.engine.campaigns.CampaignMessageStatus;
import com.whatsapp.engine.campaigns.CampaignStatus;
import com.whatsapp.engine.campaigns.repository.CampaignMessageRepository;
import com.whatsapp.engine.campaigns.repository.CampaignRepository;
import com.whatsapp.engine.common.exception.ApplicationException;
import com.whatsapp.engine.messaging.Message;
import com.whatsapp.engine.messaging.MessageDirection;
import com.whatsapp.engine.messaging.MessageStatus;
import com.whatsapp.engine.messaging.MessageType;
import com.whatsapp.engine.messaging.client.MetaWhatsAppClient;
import com.whatsapp.engine.messaging.client.MetaWhatsAppSendResponse;
import com.whatsapp.engine.messaging.repository.MessageRepository;
import com.whatsapp.engine.organization.Organization;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class CampaignProcessor {

    private static final int MAX_RETRIES = 3;

    private final CampaignMessageRepository campaignMessageRepository;
    private final CampaignRepository campaignRepository;
    private final MessageRepository messageRepository;
    private final MetaWhatsAppClient metaWhatsAppClient;
    private final ObjectMapper objectMapper;

    public CampaignProcessor(
            CampaignMessageRepository campaignMessageRepository,
            CampaignRepository campaignRepository,
            MessageRepository messageRepository,
            MetaWhatsAppClient metaWhatsAppClient,
            ObjectMapper objectMapper
    ) {
        this.campaignMessageRepository = campaignMessageRepository;
        this.campaignRepository = campaignRepository;
        this.messageRepository = messageRepository;
        this.metaWhatsAppClient = metaWhatsAppClient;
        this.objectMapper = objectMapper;
    }

    @Async
    @Transactional
    public void processCampaignAsync(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "CAMPAIGN_NOT_FOUND", "Campaign not found"));
        try {
            validateWhatsAppConfiguration(campaign.getOrganization());

            List<CampaignMessage> targets = campaignMessageRepository.findByCampaignIdAndStatusIn(
                    campaignId,
                    List.of(CampaignMessageStatus.PENDING, CampaignMessageStatus.FAILED)
            );

            log.info("Campaign processing started. campaignId={}, recipients={}", campaignId, targets.size());
            for (CampaignMessage campaignMessage : targets) {
                if (campaignMessage.getStatus() == CampaignMessageStatus.FAILED
                        && campaignMessage.getRetryCount() >= MAX_RETRIES) {
                    continue;
                }
                sendCampaignMessage(campaign, campaignMessage);
            }

            refreshAnalytics(campaignId);
            log.info("Campaign processing finished. campaignId={}", campaignId);
        } catch (RuntimeException exception) {
            campaign.setStatus(CampaignStatus.FAILED);
            campaign.setCompletedAt(Instant.now());
            campaignRepository.save(campaign);
            log.warn("Campaign processing failed. campaignId={}, reason={}", campaignId, exception.getMessage());
        }
    }

    private void sendCampaignMessage(Campaign campaign, CampaignMessage campaignMessage) {
        campaignMessage.setLastAttemptAt(Instant.now());
        campaignMessage.setRetryCount(campaignMessage.getRetryCount() + 1);

        Message message = buildMessage(campaign, campaignMessage);
        messageRepository.save(message);

        try {
            MetaWhatsAppSendResponse metaResponse = sendViaMeta(campaign, campaignMessage);
            message.setMetaMessageId(metaResponse.messageId());
            message.setMetaResponse(metaResponse.rawResponse());
            message.setStatus(MessageStatus.ACCEPTED);
            message.setSentAt(Instant.now());
            messageRepository.save(message);

            campaignMessage.setMessage(message);
            campaignMessage.setStatus(CampaignMessageStatus.SENT);
            campaignMessage.setFailureReason(null);
            campaignMessage.setSentAt(message.getSentAt());
            campaignMessageRepository.save(campaignMessage);
        } catch (RuntimeException exception) {
            message.setStatus(MessageStatus.FAILED);
            message.setFailureReason(exception.getMessage());
            message.setFailedAt(Instant.now());
            messageRepository.save(message);

            campaignMessage.setMessage(message);
            campaignMessage.setStatus(CampaignMessageStatus.FAILED);
            campaignMessage.setFailureReason(exception.getMessage());
            campaignMessageRepository.save(campaignMessage);
            log.warn(
                    "Campaign message failed. campaignId={}, contactId={}, attempt={}, reason={}",
                    campaign.getId(),
                    campaignMessage.getContact().getId(),
                    campaignMessage.getRetryCount(),
                    exception.getMessage()
            );
        }
    }

    private Message buildMessage(Campaign campaign, CampaignMessage campaignMessage) {
        Message message = new Message();
        message.setOrganization(campaign.getOrganization());
        message.setContact(campaignMessage.getContact());
        message.setCampaign(campaign);
        message.setRecipientPhoneNumber(campaignMessage.getContact().getPhoneNumber());
        message.setDirection(MessageDirection.OUTBOUND);
        message.setMessageType(campaign.getMessageType());
        message.setStatus(MessageStatus.ACCEPTED);
        message.setTextBody(campaign.getTextBody());
        message.setTemplateName(campaign.getTemplateName());
        message.setTemplateLanguage(campaign.getTemplateLanguage());
        message.setTemplateParameters(campaign.getTemplateParameters());
        return message;
    }

    private MetaWhatsAppSendResponse sendViaMeta(Campaign campaign, CampaignMessage campaignMessage) {
        Organization organization = campaign.getOrganization();
        if (campaign.getMessageType() == MessageType.TEXT) {
            return metaWhatsAppClient.sendTextMessage(
                    organization.getWhatsappPhoneNumberId(),
                    organization.getWhatsappAccessToken(),
                    campaignMessage.getContact().getPhoneNumber(),
                    campaign.getTextBody()
            );
        }

        return metaWhatsAppClient.sendTemplateMessage(
                organization.getWhatsappPhoneNumberId(),
                organization.getWhatsappAccessToken(),
                campaignMessage.getContact().getPhoneNumber(),
                campaign.getTemplateName(),
                campaign.getTemplateLanguage(),
                templateParameters(campaign)
        );
    }

    private void refreshAnalytics(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "CAMPAIGN_NOT_FOUND", "Campaign not found"));
        int sent = (int) campaignMessageRepository.countByCampaignIdAndStatus(campaignId, CampaignMessageStatus.SENT);
        int failed = (int) campaignMessageRepository.countByCampaignIdAndStatus(campaignId, CampaignMessageStatus.FAILED);
        int pending = Math.max(0, campaign.getTotalRecipients() - sent - failed);

        campaign.setSentCount(sent);
        campaign.setFailedCount(failed);
        if (pending == 0) {
            campaign.setStatus(failed == 0 ? CampaignStatus.COMPLETED : CampaignStatus.PARTIALLY_FAILED);
            campaign.setCompletedAt(Instant.now());
        } else if (sent > 0 || failed > 0) {
            campaign.setStatus(CampaignStatus.PARTIALLY_FAILED);
        } else {
            campaign.setStatus(CampaignStatus.FAILED);
            campaign.setCompletedAt(Instant.now());
        }
        campaignRepository.save(campaign);
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

    private List<String> templateParameters(Campaign campaign) {
        try {
            return objectMapper.readValue(campaign.getTemplateParameters(), new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "INVALID_TEMPLATE_PARAMETERS", "Invalid template parameters");
        }
    }
}
