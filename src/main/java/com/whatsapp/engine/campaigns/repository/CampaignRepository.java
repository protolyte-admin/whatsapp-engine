package com.whatsapp.engine.campaigns.repository;

import com.whatsapp.engine.campaigns.Campaign;
import com.whatsapp.engine.campaigns.CampaignStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    Page<Campaign> findByOrganizationId(UUID organizationId, Pageable pageable);

    @EntityGraph(attributePaths = "organization")
    Optional<Campaign> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @EntityGraph(attributePaths = "organization")
    List<Campaign> findTop25ByStatusInAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            Collection<CampaignStatus> statuses,
            Instant scheduledAt
    );
}
