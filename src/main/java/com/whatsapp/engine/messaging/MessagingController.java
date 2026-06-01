package com.whatsapp.engine.messaging;

import com.whatsapp.engine.auth.User;
import com.whatsapp.engine.common.api.ApiResponse;
import com.whatsapp.engine.messaging.dto.MessageResponse;
import com.whatsapp.engine.messaging.dto.SendBulkTextMessageRequest;
import com.whatsapp.engine.messaging.dto.SendTemplateMessageRequest;
import com.whatsapp.engine.messaging.dto.SendTextMessageRequest;
import com.whatsapp.engine.messaging.service.MessagingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class MessagingController {

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @PostMapping("/text")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'AGENT')")
    public ResponseEntity<ApiResponse<MessageResponse>> sendTextMessage(
            @Valid @RequestBody SendTextMessageRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success("Text message sent", messagingService.sendTextMessage(request, user)));
    }

    @PostMapping("/bulk/text")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'AGENT')")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> sendBulkTextMessage(
            @Valid @RequestBody SendBulkTextMessageRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success("Text message sent", messagingService.sendBulkTextMessage(request, user)));
    }

    @PostMapping("/template")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'AGENT')")
    public ResponseEntity<ApiResponse<MessageResponse>> sendTemplateMessage(
            @Valid @RequestBody SendTemplateMessageRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Template message sent",
                messagingService.sendTemplateMessage(request, user)
        ));
    }
}
