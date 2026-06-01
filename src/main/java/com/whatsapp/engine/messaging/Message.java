package com.whatsapp.engine.messaging;

import com.whatsapp.engine.auth.User;
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
@Table(name = "messages")
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(nullable = false, length = 32)
    private String recipientPhoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MessageDirection direction = MessageDirection.OUTBOUND;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MessageStatus status = MessageStatus.PENDING;

    @Column(length = 120)
    private String metaMessageId;

    @Column(columnDefinition = "TEXT")
    private String textBody;

    @Column(length = 150)
    private String templateName;

    @Column(length = 20)
    private String templateLanguage;

    @Column(columnDefinition = "TEXT")
    private String templateParameters;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @Column(columnDefinition = "TEXT")
    private String metaResponse;

    @Column(columnDefinition = "TEXT")
    private String rawWebhookPayload;

    private Instant sentAt;

    private Instant deliveredAt;

    private Instant readAt;
}
