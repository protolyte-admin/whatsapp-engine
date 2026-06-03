package com.whatsapp.engine.messaging.repository;

import com.whatsapp.engine.messaging.MessageReport;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReportRepository extends JpaRepository<MessageReport, UUID> {
}
