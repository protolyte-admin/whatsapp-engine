package com.whatsapp.engine.webhooks;

import com.whatsapp.engine.common.entity.BaseEntity;
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
@Table(name = "webhook_events")
public class WebhookEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false, length = 80)
    private String provider;

    @Column(length = 80)
    private String phoneNumberId;

    @Column(length = 120)
    private String metaMessageId;

    @Column(length = 80)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WebhookEventStatus status = WebhookEventStatus.RECEIVED;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Instant processedAt;
}
