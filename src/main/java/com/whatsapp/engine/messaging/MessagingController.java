package com.whatsapp.engine.messaging;

import com.whatsapp.engine.auth.User;
import com.whatsapp.engine.common.api.ApiResponse;
import com.whatsapp.engine.messaging.dto.*;
import com.whatsapp.engine.messaging.service.MessagingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @PostMapping("/bulk/template")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'AGENT')")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> sendBulkTemplateMessage(
            @Valid @RequestBody SendBulkTemplateMessageRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success("Template message sent",
                messagingService.sendBulkTemplateMessage(request, user)
        ));
    }

    @GetMapping("/conversations")
    public List<ConversationResponse> getConversations(
            @AuthenticationPrincipal User user) {

        return messagingService.getConversations(
                user);
    }

    @GetMapping("/conversation/{phoneNumber}/messages")
    public List<ConversationMessageResponse> getMessageList(@PathVariable String phoneNumber, @AuthenticationPrincipal User user) {
        return messagingService.getMessagesByPhoneNumber(phoneNumber, user);
    }
}
