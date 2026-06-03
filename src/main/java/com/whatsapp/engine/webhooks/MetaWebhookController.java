package com.whatsapp.engine.webhooks;

import com.whatsapp.engine.webhooks.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Meta Webhooks", description = "Meta WhatsApp webhook verification and callback APIs")
@RestController
@RequestMapping("/webhooks/meta")
public class MetaWebhookController {

    private final WebhookService webhookService;

    public MetaWebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Operation(summary = "Verify Meta webhook subscription")
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        return ResponseEntity.ok(webhookService.verifyMetaWebhook(mode, verifyToken, challenge));
    }

    @Operation(summary = "Receive Meta WhatsApp webhook callback")
    @PostMapping
    public ResponseEntity<Void> receiveWebhook(@RequestBody String payload) {
        webhookService.receiveMetaWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
