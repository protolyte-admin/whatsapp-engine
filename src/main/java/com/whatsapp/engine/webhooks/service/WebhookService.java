package com.whatsapp.engine.webhooks.service;

import com.whatsapp.engine.common.exception.ApplicationException;
import com.whatsapp.engine.messaging.client.MetaWhatsAppProperties;
import com.whatsapp.engine.webhooks.WebhookEvent;
import com.whatsapp.engine.webhooks.repository.WebhookEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class WebhookService {

    private static final String PROVIDER_META = "META_WHATSAPP";

    private final MetaWhatsAppProperties metaWhatsAppProperties;
    private final WebhookEventProcessor webhookEventProcessor;
    private final WebhookEventRepository webhookEventRepository;

    public WebhookService(
            MetaWhatsAppProperties metaWhatsAppProperties,
            WebhookEventProcessor webhookEventProcessor,
            WebhookEventRepository webhookEventRepository
    ) {
        this.metaWhatsAppProperties = metaWhatsAppProperties;
        this.webhookEventProcessor = webhookEventProcessor;
        this.webhookEventRepository = webhookEventRepository;
    }

    public String verifyMetaWebhook(String mode, String verifyToken, String challenge) {
        if ("subscribe".equals(mode)
                && StringUtils.hasText(challenge)
                && metaWhatsAppProperties.getWebhookVerifyToken().equals(verifyToken)) {
            log.info("Meta webhook verification succeeded");
            return challenge;
        }

        log.warn("Meta webhook verification failed. mode={}", mode);
        throw new ApplicationException(HttpStatus.FORBIDDEN, "WEBHOOK_VERIFICATION_FAILED", "Invalid webhook verification token");
    }

    public void receiveMetaWebhook(String payload) {
        WebhookEvent event = new WebhookEvent();
        event.setProvider(PROVIDER_META);
        event.setRawPayload(payload);
        WebhookEvent savedEvent = webhookEventRepository.save(event);

        log.info("Meta webhook received. eventId={}", savedEvent.getId());
        webhookEventProcessor.processMetaWebhookAsync(savedEvent.getId());
    }
}
