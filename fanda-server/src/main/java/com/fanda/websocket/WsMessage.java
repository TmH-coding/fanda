package com.fanda.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsMessage {

    public enum Type {
        JOIN,    // 有人加入
        VOTE,    // 投票更新
        CHAT,    // 聊天消息
        FULL,    // 群满员
        INFO     // 系统通知
    }

    private Type type;
    private Long groupId;
    private String sender;
    private String content;
    private Object data;      // 携带的业务数据（如最新候选票数）
    private LocalDateTime timestamp;

    public static WsMessage join(Long groupId, String sender) {
        return WsMessage.builder()
                .type(Type.JOIN)
                .groupId(groupId)
                .sender(sender)
                .content(sender + " 加入了拼饭")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WsMessage vote(Long groupId, String sender, String candidateName, Object candidates) {
        return WsMessage.builder()
                .type(Type.VOTE)
                .groupId(groupId)
                .sender(sender)
                .content(sender + " 投票给 " + candidateName)
                .data(candidates)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WsMessage chat(Long groupId, String sender, String content) {
        return WsMessage.builder()
                .type(Type.CHAT)
                .groupId(groupId)
                .sender(sender)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static WsMessage full(Long groupId) {
        return WsMessage.builder()
                .type(Type.FULL)
                .groupId(groupId)
                .content("拼饭已满员，即将开始！")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
