package com.whatsapp.engine.common.audit;

import com.whatsapp.engine.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Column(length = 120)
    private String correlationId;

    @Column(length = 20)
    private String method;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(length = 80)
    private String action;

    @Column(length = 180)
    private String username;

    private UUID userId;

    private UUID organizationId;

    @Column(length = 80)
    private String clientIp;

    @Column(length = 300)
    private String userAgent;

    private Integer statusCode;

    private Long durationMs;

    private Instant occurredAt;
}
