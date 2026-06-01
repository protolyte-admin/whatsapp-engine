package com.whatsapp.engine.contacts;

import com.whatsapp.engine.common.entity.BaseEntity;
import com.whatsapp.engine.organization.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "contacts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_contacts_organization_phone", columnNames = {"organization_id", "phone_number"})
        }
)
public class Contact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "phone_number", nullable = false, length = 32)
    private String phoneNumber;

    @Column(length = 180)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean active = true;
}
