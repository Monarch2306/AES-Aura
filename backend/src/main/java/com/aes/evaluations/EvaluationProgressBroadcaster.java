package com.aes.evaluations;

import com.aes.evaluations.dto.EvaluationProgressEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@SuppressWarnings("null")
public class EvaluationProgressBroadcaster extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(EvaluationProgressBroadcaster.class);

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Map<Long, Set<WebSocketSession>> sheetSessions = new ConcurrentHashMap<>();
    private final Set<WebSocketSession> globalSessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long sheetId = extractSheetId(session.getUri());
        if (sheetId != null) {
            sheetSessions.computeIfAbsent(sheetId, k -> new CopyOnWriteArraySet<>()).add(session);
            log.info("WebSocket client connected for sheetId={}: session={}", sheetId, session.getId());
        } else {
            globalSessions.add(session);
            log.info("WebSocket client connected for global evaluations: session={}", session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            if (node.has("sheetId")) {
                Long sheetId = node.get("sheetId").asLong();
                sheetSessions.computeIfAbsent(sheetId, k -> new CopyOnWriteArraySet<>()).add(session);
                log.info("WebSocket session {} explicitly subscribed to sheetId={}", session.getId(), sheetId);
            }
        } catch (Exception e) {
            log.debug("Unrecognized WebSocket text frame: {}", message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        globalSessions.remove(session);
        sheetSessions.values().forEach(sessions -> sessions.remove(session));
        log.debug("WebSocket session closed: {}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException ignored) {}
    }

    public void broadcast(Long sheetId, EvaluationProgressEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            TextMessage message = new TextMessage(payload);

            Set<WebSocketSession> targeted = sheetSessions.get(sheetId);
            if (targeted != null) {
                for (WebSocketSession session : targeted) {
                    sendSafe(session, message);
                }
            }

            for (WebSocketSession session : globalSessions) {
                sendSafe(session, message);
            }
        } catch (Exception e) {
            log.error("Failed to broadcast progress event for sheetId={}", sheetId, e);
        }
    }

    private void sendSafe(WebSocketSession session, TextMessage message) {
        if (session.isOpen()) {
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                log.debug("Failed sending WebSocket message to session {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    private Long extractSheetId(URI uri) {
        if (uri == null) return null;
        String path = uri.getPath();
        if (path != null) {
            String[] parts = path.split("/");
            if (parts.length > 0) {
                try {
                    return Long.parseLong(parts[parts.length - 1]);
                } catch (NumberFormatException ignored) {}
            }
        }
        String query = uri.getQuery();
        if (query != null && query.contains("sheetId=")) {
            for (String param : query.split("&")) {
                if (param.startsWith("sheetId=")) {
                    try {
                        return Long.parseLong(param.substring("sheetId=".length()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return null;
    }
}