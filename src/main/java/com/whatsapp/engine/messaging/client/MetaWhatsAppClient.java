package com.whatsapp.engine.messaging.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.engine.common.exception.ApplicationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
public class MetaWhatsAppClient {

    private final ObjectMapper objectMapper;
    private final MetaWhatsAppProperties properties;
    private final WebClient webClient;

    public MetaWhatsAppClient(
            ObjectMapper objectMapper,
            MetaWhatsAppProperties properties,
            WebClient.Builder webClientBuilder
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.webClient = webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public MetaWhatsAppSendResponse sendTextMessage(
            String phoneNumberId,
            String accessToken,
            String to,
            String body
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", to);
        payload.put("type", "text");
        payload.put("text", Map.of("preview_url", false, "body", body));

        return sendMessage(phoneNumberId, accessToken, payload);
    }

    public MetaWhatsAppSendResponse sendTemplateMessage(
            String phoneNumberId,
            String accessToken,
            String to,
            String templateName,
            String languageCode,
            List<String> bodyParameters
    ) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", templateName);
        template.put("language", Map.of("code", languageCode));

        if (bodyParameters != null && !bodyParameters.isEmpty()) {
            List<Map<String, Object>> parameters = bodyParameters.stream()
                    .map(parameter -> Map.<String, Object>of("type", "text", "text", parameter))
                    .toList();
            template.put("components", List.of(Map.of(
                    "type", "body",
                    "parameters", parameters
            )));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", "template");
        payload.put("template", template);

        return sendMessage(phoneNumberId, accessToken, payload);
    }

    private MetaWhatsAppSendResponse sendMessage(
            String phoneNumberId,
            String accessToken,
            Map<String, Object> payload
    ) {
        String path = "/%s/%s/messages".formatted(properties.getApiVersion(), phoneNumberId);
        try {
            String response = webClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) {
                throw new ApplicationException(HttpStatus.BAD_GATEWAY, "META_RESPONSE_EMPTY", "Meta response was empty");
            }
            String messageId = extractMessageId(response);
            log.info("Meta WhatsApp message accepted. phoneNumberId={}, metaMessageId={}", phoneNumberId, messageId);
            return new MetaWhatsAppSendResponse(messageId, response);
        } catch (WebClientResponseException exception) {
            log.warn(
                    "Meta WhatsApp API rejected message. status={}, body={}",
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString()
            );
            throw new ApplicationException(
                    HttpStatus.BAD_GATEWAY,
                    "META_WHATSAPP_ERROR",
                    "Meta WhatsApp API rejected the message"
            );
        } catch (WebClientException exception) {
            log.warn("Meta WhatsApp API request failed: {}", exception.getMessage());
            throw new ApplicationException(
                    HttpStatus.BAD_GATEWAY,
                    "META_WHATSAPP_UNAVAILABLE",
                    "Meta WhatsApp API is unavailable"
            );
        }
    }

    private String extractMessageId(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode messages = root.path("messages");
            if (messages.isArray() && messages.size() > 0) {
                String messageId = messages.get(0).path("id").asText(null);
                if (messageId != null) {
                    return messageId;
                }
            }
            throw new ApplicationException(HttpStatus.BAD_GATEWAY, "META_MESSAGE_ID_MISSING", "Meta response did not include a message ID");
        } catch (JsonProcessingException exception) {
            throw new ApplicationException(HttpStatus.BAD_GATEWAY, "META_RESPONSE_INVALID", "Invalid Meta response");
        }
    }
}
