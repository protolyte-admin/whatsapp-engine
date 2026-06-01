package com.whatsapp.engine.campaigns.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.engine.auth.User;
import com.whatsapp.engine.campaigns.Campaign;
import com.whatsapp.engine.campaigns.CampaignMessage;
import com.whatsapp.engine.campaigns.CampaignStatus;
import com.whatsapp.engine.campaigns.dto.CampaignResponse;
import com.whatsapp.engine.campaigns.dto.CreateCampaignRequest;
import com.whatsapp.engine.campaigns.repository.CampaignMessageRepository;
import com.whatsapp.engine.campaigns.repository.CampaignRepository;
import com.whatsapp.engine.common.exception.ApplicationException;
import com.whatsapp.engine.contacts.Contact;
import com.whatsapp.engine.contacts.repository.ContactRepository;
import com.whatsapp.engine.messaging.MessageType;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CampaignService {

    private final CampaignMessageRepository campaignMessageRepository;
    private final CampaignProcessor campaignProcessor;
    private final CampaignRepository campaignRepository;
    private final ContactRepository contactRepository;
    private final ObjectMapper objectMapper;

    public CampaignService(
            CampaignMessageRepository campaignMessageRepository,
            CampaignProcessor campaignProcessor,
            CampaignRepository campaignRepository,
            ContactRepository contactRepository,
            ObjectMapper objectMapper
    ) {
        this.campaignMessageRepository = campaignMessageRepository;
        this.campaignProcessor = campaignProcessor;
        this.campaignRepository = campaignRepository;
        this.contactRepository = contactRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request, User user) {
        validateCampaignPayload(request);
        List<UUID> contactIds = distinctContactIds(request.contactIds());
        List<Contact> contacts = contactRepository.findByOrganizationIdAndIdIn(user.getOrganization().getId(), contactIds);
        if (contacts.size() != contactIds.size()) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "CONTACTS_INVALID", "One or more contacts do not belong to this organization");
        }

        Campaign campaign = new Campaign();
        campaign.setOrganization(user.getOrganization());
        campaign.setName(request.name().trim());
        campaign.setMessageType(request.messageType());
        campaign.setTextBody(StringUtils.hasText(request.textBody()) ? request.textBody().trim() : null);
        campaign.setTemplateName(StringUtils.hasText(request.templateName()) ? request.templateName().trim() : null);
        campaign.setTemplateLanguage(StringUtils.hasText(request.templateLanguage()) ? request.templateLanguage().trim() : null);
        campaign.setTemplateParameters(toJson(request.templateParameters() == null ? List.of() : request.templateParameters()));
        campaign.setScheduledAt(request.scheduledAt());
        campaign.setStatus(resolveInitialStatus(request.scheduledAt()));
        campaign.setTotalRecipients(contacts.size());
        Campaign savedCampaign = campaignRepository.save(campaign);

        List<CampaignMessage> targets = contacts.stream()
                .map(contact -> {
                    CampaignMessage campaignMessage = new CampaignMessage();
                    campaignMessage.setCampaign(savedCampaign);
                    campaignMessage.setContact(contact);
                    return campaignMessage;
                })
                .toList();
        campaignMessageRepository.saveAll(targets);

        return toResponse(savedCampaign);
    }

    @Transactional(readOnly = true)
    public Page<CampaignResponse> getCampaigns(Pageable pageable, User user) {
        return campaignRepository.findByOrganizationId(user.getOrganization().getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(UUID id, User user) {
        return toResponse(findTenantCampaign(id, user.getOrganization().getId()));
    }

    @Transactional
    public CampaignResponse launchCampaign(UUID id, User user) {
        Campaign campaign = findTenantCampaign(id, user.getOrganization().getId());
        if (campaign.getStatus() == CampaignStatus.RUNNING) {
            throw new ApplicationException(HttpStatus.CONFLICT, "CAMPAIGN_RUNNING", "Campaign is already running");
        }
        if (campaign.getStatus() == CampaignStatus.COMPLETED) {
            throw new ApplicationException(HttpStatus.CONFLICT, "CAMPAIGN_COMPLETED", "Campaign is already completed");
        }

        campaign.setStatus(CampaignStatus.RUNNING);
        campaign.setLaunchedAt(Instant.now());
        campaign.setCompletedAt(null);
        Campaign savedCampaign = campaignRepository.save(campaign);
        campaignProcessor.processCampaignAsync(savedCampaign.getId());
        return toResponse(savedCampaign);
    }

    @Transactional
    public void launchDueCampaigns() {
        List<Campaign> campaigns = campaignRepository.findTop25ByStatusInAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                List.of(CampaignStatus.SCHEDULED),
                Instant.now()
        );

        for (Campaign campaign : campaigns) {
            campaign.setStatus(CampaignStatus.RUNNING);
            campaign.setLaunchedAt(Instant.now());
            campaign.setCompletedAt(null);
            campaignRepository.save(campaign);
            campaignProcessor.processCampaignAsync(campaign.getId());
        }
    }

    private Campaign findTenantCampaign(UUID id, UUID organizationId) {
        return campaignRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "CAMPAIGN_NOT_FOUND", "Campaign not found"));
    }

    private void validateCampaignPayload(CreateCampaignRequest request) {
        if (request.messageType() == MessageType.TEXT && !StringUtils.hasText(request.textBody())) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "TEXT_BODY_REQUIRED", "Text body is required for text campaigns");
        }
        if (request.messageType() == MessageType.TEMPLATE
                && (!StringUtils.hasText(request.templateName()) || !StringUtils.hasText(request.templateLanguage()))) {
            throw new ApplicationException(
                    HttpStatus.BAD_REQUEST,
                    "TEMPLATE_REQUIRED",
                    "Template name and language are required for template campaigns"
            );
        }
    }

    private CampaignStatus resolveInitialStatus(Instant scheduledAt) {
        if (scheduledAt != null && scheduledAt.isAfter(Instant.now())) {
            return CampaignStatus.SCHEDULED;
        }
        return CampaignStatus.DRAFT;
    }

    private List<UUID> distinctContactIds(List<UUID> contactIds) {
        Set<UUID> seen = new HashSet<>(contactIds);
        if (seen.size() != contactIds.size()) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "DUPLICATE_CONTACTS", "Campaign contact list contains duplicates");
        }
        return contactIds;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "INVALID_TEMPLATE_PARAMETERS", "Invalid template parameters");
        }
    }

    CampaignResponse toResponse(Campaign campaign) {
        int pendingCount = Math.max(0, campaign.getTotalRecipients() - campaign.getSentCount() - campaign.getFailedCount());
        return new CampaignResponse(
                campaign.getId(),
                campaign.getOrganization().getId(),
                campaign.getName(),
                campaign.getMessageType(),
                campaign.getStatus(),
                campaign.getScheduledAt(),
                campaign.getLaunchedAt(),
                campaign.getCompletedAt(),
                new CampaignResponse.CampaignAnalytics(
                        campaign.getTotalRecipients(),
                        campaign.getSentCount(),
                        campaign.getFailedCount(),
                        pendingCount
                )
        );
    }
}
