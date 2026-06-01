package com.whatsapp.engine.campaigns;

import com.whatsapp.engine.common.entity.BaseEntity;
import com.whatsapp.engine.contacts.Contact;
import com.whatsapp.engine.messaging.Message;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "campaign_messages",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_campaign_messages_campaign_contact", columnNames = {"campaign_id", "contact_id"})
        }
)
public class CampaignMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CampaignMessageStatus status = CampaignMessageStatus.PENDING;

    @Column(nullable = false)
    private int retryCount;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    private Instant sentAt;

    private Instant lastAttemptAt;
}
