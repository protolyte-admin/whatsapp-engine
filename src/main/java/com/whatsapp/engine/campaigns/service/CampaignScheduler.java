package com.whatsapp.engine.campaigns.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CampaignScheduler {

    private final CampaignService campaignService;

    public CampaignScheduler(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @Scheduled(fixedDelayString = "${app.campaigns.scheduler-delay-ms:60000}")
    public void launchDueCampaigns() {
        campaignService.launchDueCampaigns();
    }
}
