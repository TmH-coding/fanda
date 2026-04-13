package com.fanda.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class SocialRoomManager {

    // groupId → 该群所有 WebSocket 会话
    private final Map<Long, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public void join(Long groupId, WebSocketSession session) {
        rooms.computeIfAbsent(groupId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.debug("WS join room {} sessionId={}", groupId, session.getId());
    }

    public void leave(Long groupId, WebSocketSession session) {
        Set<WebSocketSession> room = rooms.get(groupId);
        if (room != null) {
            room.remove(session);
            if (room.isEmpty()) rooms.remove(groupId);
        }
    }

    public void broadcast(Long groupId, WsMessage message) {
        Set<WebSocketSession> room = rooms.get(groupId);
        if (room == null || room.isEmpty()) return;
        try {
            String json = mapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession session : room) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("WS send failed sessionId={}", session.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("WS broadcast error", e);
        }
    }

    public int roomSize(Long groupId) {
        Set<WebSocketSession> room = rooms.get(groupId);
        return room == null ? 0 : room.size();
    }
}
