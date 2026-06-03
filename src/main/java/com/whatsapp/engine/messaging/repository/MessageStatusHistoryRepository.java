package com.whatsapp.engine.messaging.repository;

import com.whatsapp.engine.messaging.MessageStatus;
import com.whatsapp.engine.messaging.MessageStatusHistory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageStatusHistoryRepository extends JpaRepository<MessageStatusHistory, UUID> {

    boolean existsByMessageIdAndNewStatusAndStatusTimestamp(UUID messageId, MessageStatus newStatus, Instant statusTimestamp);

    List<MessageStatusHistory> findByMessageIdOrderByStatusTimestampAsc(UUID messageId);

    @Modifying
    @Query(value = """
            insert into message_status_history (
                id,
                message_id,
                previous_status,
                new_status,
                webhook_payload,
                to_phone_number,
                status_timestamp,
                created_at,
                updated_at
            )
            values (
                :id,
                :messageId,
                :previousStatus,
                :newStatus,
                :webhookPayload,
                :toPhoneNumber,
                :statusTimestamp,
                :now,
                :now
            )
            on conflict on constraint uk_message_status_history_transition do nothing
            """, nativeQuery = true)
    int insertStatusHistoryIfAbsent(
            @Param("id") UUID id,
            @Param("messageId") UUID messageId,
            @Param("previousStatus") String previousStatus,
            @Param("newStatus") String newStatus,
            @Param("webhookPayload") String webhookPayload,
            @Param("toPhoneNumber") String toPhoneNumber,
            @Param("statusTimestamp") Instant statusTimestamp,
            @Param("now") Instant now
    );
}
