package com.whatsapp.engine.organization;

import com.whatsapp.engine.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "organizations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_organizations_slug", columnNames = "slug")
        }
)
public class Organization extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 80)
    private String slug;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 80)
    private String whatsappBusinessAccountId;

    @Column(length = 80)
    private String whatsappPhoneNumberId;

    @Column(columnDefinition = "TEXT")
    private String whatsappAccessToken;
}
