package com.fanda.websocket;

import com.fanda.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialWebSocketHandler extends TextWebSocketHandler {

    private final SocialRoomManager roomManager;
    private final JwtTokenProvider jwtTokenProvider;

    // session → groupId
    private final Map<String, Long> sessionGroup = new ConcurrentHashMap<>();
    // session → nickname
    private final Map<String, String> sessionName = new ConcurrentHashMap<>();

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 从 query 参数获取 token 和 groupId
        String query = session.getUri() != null ? session.getUri().getQuery() : "";
        String token = extractParam(query, "token");
        String groupIdStr = extractParam(query, "groupId");

        if (token == null || groupIdStr == null) {
            closeQuietly(session);
            return;
        }

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                closeQuietly(session);
                return;
            }
            String nickname = jwtTokenProvider.getUsernameFromToken(token);
            Long groupId = Long.parseLong(groupIdStr);

            sessionGroup.put(session.getId(), groupId);
            sessionName.put(session.getId(), nickname);
            roomManager.join(groupId, session);

            log.info("WS connected: {} joined group {}", nickname, groupId);
        } catch (Exception e) {
            log.warn("WS auth failed: {}", e.getMessage());
            closeQuietly(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long groupId = sessionGroup.get(session.getId());
        String sender = sessionName.get(session.getId());
        if (groupId == null || sender == null) return;

        try {
            JsonNode node = mapper.readTree(message.getPayload());
            String type = node.path("type").asText();

            if ("CHAT".equals(type)) {
                String content = node.path("content").asText("").trim();
                if (!content.isEmpty() && content.length() <= 200) {
                    roomManager.broadcast(groupId, WsMessage.chat(groupId, sender, content));
                }
            }
        } catch (Exception e) {
            log.warn("WS message parse error: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long groupId = sessionGroup.remove(session.getId());
        sessionName.remove(session.getId());
        if (groupId != null) {
            roomManager.leave(groupId, session);
            log.info("WS disconnected from group {}", groupId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WS transport error: {}", exception.getMessage());
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    private String extractParam(String query, String key) {
        if (query == null) return null;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && key.equals(kv[0])) return kv[1];
        }
        return null;
    }

    private void closeQuietly(WebSocketSession session) {
        try { session.close(CloseStatus.NOT_ACCEPTABLE); } catch (Exception ignored) {}
    }
}
