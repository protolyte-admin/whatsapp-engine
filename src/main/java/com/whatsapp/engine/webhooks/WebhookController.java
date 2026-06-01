package com.whatsapp.engine.webhooks;

import com.whatsapp.engine.webhooks.service.WebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/meta")
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        return ResponseEntity.ok(webhookService.verifyMetaWebhook(mode, verifyToken, challenge));
    }

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(@RequestBody String payload) {
        log.info("META WEBHOOK PAYLOAD = {}", payload);
        webhookService.receiveMetaWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
