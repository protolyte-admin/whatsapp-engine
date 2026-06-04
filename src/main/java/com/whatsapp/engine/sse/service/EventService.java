package com.whatsapp.engine.sse.service;


import com.whatsapp.engine.sse.dto.MessageEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class EventService {

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID organizationId) {

        SseEmitter emitter = new SseEmitter(0L);

        emitters.computeIfAbsent(
                        organizationId,
                        id -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() ->
                emitters.getOrDefault(
                        organizationId,
                        List.of()).remove(emitter));

        emitter.onTimeout(() ->
                emitters.getOrDefault(
                        organizationId,
                        List.of()).remove(emitter));

        return emitter;
    }

    public void publish(UUID organizationId,
                        MessageEvent event) {

        List<SseEmitter> organizationEmitters =
                emitters.getOrDefault(
                        organizationId,
                        List.of());

        for (SseEmitter emitter : organizationEmitters) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("new-message")
                                .data(event)
                );
            } catch (Exception ex) {
                emitter.complete();
            }
        }
    }
}
