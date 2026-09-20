package com.example.InkHub_backend.utils;

// Redis key 统一管理，避免散落魔法字符串
public class RedisKeys {

    // 文章浏览量计数：INCR 后定时落库，key: article:view:{articleId}
    public static String articleView(Long articleId) {
        return "article:view:" + articleId;
    }

    // AI 问答限流：每用户每分钟窗口，key: ai:chat:rate:{userId}:{yyyyMMddHHmm}
    public static String aiChatRate(Long userId, String minute) {
        return "ai:chat:rate:" + userId + ":" + minute;
    }

    // Agent 对话历史：每用户一个 List，key: ai:agent:history:{userId}
    public static String agentHistory(Long userId) {
        return "ai:agent:history:" + userId;
    }

    public static String agentHistory(Long userId, String sessionId) {
        return "ai:agent:history:" + userId + ":" + sessionId;
    }
}
