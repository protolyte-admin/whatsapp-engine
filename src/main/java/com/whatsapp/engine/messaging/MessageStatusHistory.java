package com.whatsapp.engine.messaging;

import com.whatsapp.engine.common.entity.BaseEntity;
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
        name = "message_status_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_message_status_history_transition",
                        columnNames = {"message_id", "new_status", "status_timestamp"}
                )
        }
)
public class MessageStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private MessageStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MessageStatus newStatus;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String webhookPayload;

    @Column(length = 32)
    private String toPhoneNumber;

    @Column(nullable = false)
    private Instant statusTimestamp;
}
