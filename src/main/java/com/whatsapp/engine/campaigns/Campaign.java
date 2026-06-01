package com.whatsapp.engine.campaigns;

import com.whatsapp.engine.common.entity.BaseEntity;
import com.whatsapp.engine.messaging.MessageType;
import com.whatsapp.engine.organization.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "campaigns")
public class Campaign extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String textBody;

    @Column(length = 150)
    private String templateName;

    @Column(length = 20)
    private String templateLanguage;

    @Column(columnDefinition = "TEXT")
    private String templateParameters;

    private Instant scheduledAt;

    private Instant launchedAt;

    private Instant completedAt;

    @Column(nullable = false)
    private int totalRecipients;

    @Column(nullable = false)
    private int sentCount;

    @Column(nullable = false)
    private int failedCount;
}
