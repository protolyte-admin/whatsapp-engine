package com.whatsapp.engine.webhooks.repository;

import com.whatsapp.engine.webhooks.WebhookEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
}
