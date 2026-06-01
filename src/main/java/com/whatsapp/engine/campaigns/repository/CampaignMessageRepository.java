package com.whatsapp.engine.campaigns.repository;

import com.whatsapp.engine.campaigns.CampaignMessage;
import com.whatsapp.engine.campaigns.CampaignMessageStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignMessageRepository extends JpaRepository<CampaignMessage, UUID> {

    long countByCampaignIdAndStatus(UUID campaignId, CampaignMessageStatus status);

    @EntityGraph(attributePaths = {"campaign", "campaign.organization", "contact"})
    List<CampaignMessage> findByCampaignIdAndStatusIn(UUID campaignId, Collection<CampaignMessageStatus> statuses);

    List<CampaignMessage> findByCampaignId(UUID campaignId);
}
