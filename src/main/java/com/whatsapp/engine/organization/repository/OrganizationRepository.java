package com.whatsapp.engine.organization.repository;

import com.whatsapp.engine.organization.Organization;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    boolean existsBySlugIgnoreCase(String slug);

    Optional<Organization> findBySlugIgnoreCase(String slug);

    Optional<Organization> findByWhatsappPhoneNumberId(String whatsappPhoneNumberId);
}
