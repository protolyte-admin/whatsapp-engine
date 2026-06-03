package com.whatsapp.engine.messaging;

import com.whatsapp.engine.common.entity.BaseEntity;
import com.whatsapp.engine.organization.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "message_reports")
public class MessageReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 120)
    private String reportType;

    private Instant dateFrom;

    private Instant dateTo;

    @Column(nullable = false)
    private long totalMessages;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String filters;

    @Column(nullable = false)
    private Instant generatedAt;
}
