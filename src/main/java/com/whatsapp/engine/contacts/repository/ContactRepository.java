package com.whatsapp.engine.contacts.repository;

import com.whatsapp.engine.contacts.Contact;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactRepository extends JpaRepository<Contact, UUID> {

    boolean existsByOrganizationIdAndPhoneNumber(UUID organizationId, String phoneNumber);

    Optional<Contact> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Contact> findByOrganizationIdAndPhoneNumber(UUID organizationId, String phoneNumber);

    List<Contact> findByOrganizationIdAndIdIn(UUID organizationId, Collection<UUID> ids);

    Page<Contact> findByOrganizationId(UUID organizationId, Pageable pageable);

    @Query("""
            select contact
            from Contact contact
            where contact.organization.id = :organizationId
              and (
                    lower(contact.name) like lower(concat('%', :search, '%'))
                    or contact.phoneNumber like concat('%', :search, '%')
              )
            """)
    Page<Contact> searchByNameOrPhone(
            @Param("organizationId") UUID organizationId,
            @Param("search") String search,
            Pageable pageable
    );
}
