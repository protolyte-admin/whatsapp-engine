package com.whatsapp.engine.messaging.repository;

import com.whatsapp.engine.messaging.Message;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Optional<Message> findByMetaMessageId(String metaMessageId);
}
