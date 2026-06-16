package com.whatsapp.engine.messaging.repository;

import com.whatsapp.engine.messaging.Message;
import com.whatsapp.engine.messaging.MessageStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.whatsapp.engine.messaging.reports.MessageRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

//@EnableJpaRepositories
public interface MessageRepository extends JpaRepository<Message, UUID>, MessageRepositoryCustom {

    Optional<Message> findByMetaMessageId(String metaMessageId);

    Optional<Message> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Message> findAllByOrganizationId(UUID organizationId);

    List<Message> findAllByRecipientPhoneNumberAndOrganizationIdOrderBySentAtAsc(String recipientPhoneNumber, UUID organizationId);

    @Query("""
            select message
            from Message message
            where message.organization.id = :organizationId
            and message.createdAt = (
                select max(m2.createdAt)
                from Message m2
                where m2.organization.id = :organizationId
                    and m2.recipientPhoneNumber = message.recipientPhoneNumber
               )
                order by message.createdAt desc
            """)
    List<Message> findLatestConversationMessages(
            @Param("organizationId") UUID organizationId);

    @Query("""
            select message
            from Message message
            where message.organization.id = :organizationId
              and message.createdAt >= :dateFrom
              and message.createdAt <= :dateTo
              and (:templateName is null or message.templateName = :templateName)
              and (:campaignId is null or message.campaign.id = :campaignId)
              and (:contactId is null or message.contact.id = :contactId)
              and (:status is null or message.status = :status)
            """)
    Page<Message> findReportMessages(
            @Param("organizationId") UUID organizationId,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("templateName") String templateName,
            @Param("campaignId") UUID campaignId,
            @Param("contactId") UUID contactId,
            @Param("status") MessageStatus status,
            Pageable pageable
    );


//    @Query("""
//            select
//                count(message.id),
//                sum(case when message.status in (com.whatsapp.engine.messaging.MessageStatus.ACCEPTED, com.whatsapp.engine.messaging.MessageStatus.SENT, com.whatsapp.engine.messaging.MessageStatus.DELIVERED, com.whatsapp.engine.messaging.MessageStatus.READ) then 1 else 0 end),
//                sum(case when message.status in (com.whatsapp.engine.messaging.MessageStatus.DELIVERED, com.whatsapp.engine.messaging.MessageStatus.READ) then 1 else 0 end),
//                sum(case when message.status = com.whatsapp.engine.messaging.MessageStatus.READ then 1 else 0 end),
//                sum(case when message.status = com.whatsapp.engine.messaging.MessageStatus.FAILED then 1 else 0 end),
//                sum(case when message.sentAt >= :todayStart then 1 else 0 end),
//                sum(case when message.deliveredAt >= :todayStart then 1 else 0 end),
//                sum(case when message.readAt >= :todayStart then 1 else 0 end)
//            from Message message
//            where message.organization.id = :organizationId
//              and message.direction = com.whatsapp.engine.messaging.MessageDirection.OUTBOUND
//              and (:dateFrom is null or message.createdAt >= :dateFrom)
//              and (:dateTo is null or message.createdAt <= :dateTo)
//              and (:templateName is null or message.templateName = :templateName)
//              and (:campaignId is null or message.campaign.id = :campaignId)
//              and (:contactId is null or message.contact.id = :contactId)
//              and (:status is null or message.status = :status)
//            """)
//    Object[] getSummaryMetrics(
//            @Param("organizationId") UUID organizationId,
//            @Param("dateFrom") Instant dateFrom,
//            @Param("dateTo") Instant dateTo,
//            @Param("templateName") String templateName,
//            @Param("campaignId") UUID campaignId,
//            @Param("contactId") UUID contactId,
//            @Param("status") MessageStatus status,
//            @Param("todayStart") Instant todayStart
//    );

    @Query("""
            select message.status, count(message.id)
            from Message message
            where message.organization.id = :organizationId
              and message.direction = com.whatsapp.engine.messaging.MessageDirection.OUTBOUND
              and (:dateFrom is null or message.createdAt >= :dateFrom)
              and (:dateTo is null or message.createdAt <= :dateTo)
              and (:templateName is null or message.templateName = :templateName)
              and (:campaignId is null or message.campaign.id = :campaignId)
              and (:contactId is null or message.contact.id = :contactId)
              and (:status is null or message.status = :status)
            group by message.status
            """)
    List<Object[]> getStatusBreakdown(
            @Param("organizationId") UUID organizationId,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("templateName") String templateName,
            @Param("campaignId") UUID campaignId,
            @Param("contactId") UUID contactId,
            @Param("status") MessageStatus status
    );

    @Query("""
            select cast(message.createdAt as date), count(message.id)
            from Message message
            where message.organization.id = :organizationId
              and message.direction = com.whatsapp.engine.messaging.MessageDirection.OUTBOUND
              and (:dateFrom is null or message.createdAt >= :dateFrom)
              and (:dateTo is null or message.createdAt <= :dateTo)
              and (:templateName is null or message.templateName = :templateName)
              and (:campaignId is null or message.campaign.id = :campaignId)
              and (:contactId is null or message.contact.id = :contactId)
              and (:status is null or message.status = :status)
            group by cast(message.createdAt as date)
            order by cast(message.createdAt as date)
            """)
    List<Object[]> getDailyTrend(
            @Param("organizationId") UUID organizationId,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("templateName") String templateName,
            @Param("campaignId") UUID campaignId,
            @Param("contactId") UUID contactId,
            @Param("status") MessageStatus status
    );
}
