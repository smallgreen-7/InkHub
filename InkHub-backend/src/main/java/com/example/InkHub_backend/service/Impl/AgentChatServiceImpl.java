package com.example.InkHub_backend.service.Impl;

import com.example.InkHub_backend.common.BusinessException;
import com.example.InkHub_backend.config.AiProperties;
import com.example.InkHub_backend.dto.AgentChatRequest;
import com.example.InkHub_backend.service.AgentChatService;
import com.example.InkHub_backend.utils.RedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 对话实现（修正版 2026-09-08）
 *
 * 和普通 RAG 问答的区别：
 * - 普通 RAG：检索→拼提示词→回答（只能基于站内文章内容回答）
 * - Agent：LLM 自主判断是否调用工具（如查热门文章、查统计数据）→ 基于工具结果组织回答
 *
 * 对话记忆：Redis List 按 (userId, sessionId) 存最近 N 轮，对话前加载、对话后追加
 * 清空会话：前端换新 sessionId 即可（旧 key 2h TTL 自动过期）
 *
 * SSE 事件：
 * {"type":"delta","text":"..."}    —— 流式文本片段
 * {"type":"done"}                  —— 完成
 * {"type":"error","msg":"..."}     —— 错误
 */
@Slf4j
@Service
public class AgentChatServiceImpl implements AgentChatService {

    private static final DateTimeFormatter MINUTE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /** 记忆保留条数：6 轮 = 12 条（user + assistant 各一） */
    private static final long HISTORY_KEEP = 12;
    /** 记忆 TTL：2 小时无对话自动过期 */
    private static final Duration HISTORY_TTL = Duration.ofHours(2);

    private final ChatClient chatClient;
    private final AiProperties props;
    private final RedisTemplate<String, Object> redisTemplate;

    public AgentChatServiceImpl(
            @Qualifier("agentChatClient") ChatClient chatClient,
            AiProperties props,
            RedisTemplate<String, Object> redisTemplate) {
        this.chatClient = chatClient;
        this.props = props;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Async
    public void chat(Long userId, AgentChatRequest req, SseEmitter emitter) {
        try {
            checkRate(userId);
            String question = req.getQuestion().strip();
            if (question.isEmpty()) throw new BusinessException("问题不能为空");

            // 1. 会话 key：前端带 sessionId 用 sessionId，没带退回 userId（单会话兼容）
            String sessionId = (req.getSessionId() == null || req.getSessionId().isBlank())
                    ? "default" : req.getSessionId();
            String historyKey = RedisKeys.agentHistory(userId, sessionId);

            // 2. 加载对话历史（Redis）
            String history = loadHistory(historyKey);

            // 3. 构造 Prompt（带历史 + Agent 系统提示在 ChatClient 里已配）
            String userPrompt = history.isEmpty()
                    ? question
                    : history + "\n用户：" + question;

            // 4. 流式调用（Spring AI 自动处理 Function Calling）
            Flux<String> flux = chatClient.prompt()
                    .user(userPrompt)
                    .stream()
                    .content();

            StringBuilder fullResponse = new StringBuilder();

            flux.doOnNext(delta -> {
                        sendQuietly(emitter, Map.of("type", "delta", "text", delta));
                        fullResponse.append(delta);
                    })
                    .doOnComplete(() -> {
                        sendQuietly(emitter, Map.of("type", "done"));
                        emitter.complete();
                        // 保存对话历史（流式结束后才能拿到完整回答）
                        saveHistory(historyKey, question, fullResponse.toString());
                    })
                    .doOnError(e -> {
                        log.error("Agent 流式问答异常 userId={}", userId, e);
                        sendQuietly(emitter, Map.of("type", "error", "msg", "AI 服务开小差了，请稍后再试"));
                        emitter.complete();
                    })
                    .subscribe();
        } catch (BusinessException e) {
            sendQuietly(emitter, Map.of("type", "error", "msg", e.getMessage()));
            emitter.complete();
        } catch (Exception e) {
            log.error("Agent 问答异常 userId={}", userId, e);
            sendQuietly(emitter, Map.of("type", "error", "msg", "AI 服务开小差了，请稍后再试"));
            emitter.complete();
        }
    }

    // ========== 私有方法 ==========

    private void checkRate(Long userId) {
        if (userId == null) return;
        String key = RedisKeys.aiChatRate(userId, LocalDateTime.now().format(MINUTE_FMT));
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(60));
        }
        if (count != null && count > props.getRateLimit()) {
            throw new BusinessException(429, "提问太频繁啦，歇一分钟再问吧");
        }
    }

    /** 加载最近 6 轮对话历史（Redis List，取最后 HISTORY_KEEP 条） */
    @SuppressWarnings("unchecked")
    private String loadHistory(String key) {
        List<Object> raw = redisTemplate.opsForList().range(key, -HISTORY_KEEP, -1);
        if (raw == null || raw.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("【对话历史】\n");
        for (Object item : raw) {
            sb.append(item).append("\n");
        }
        return sb.toString();
    }

    /** 保存对话历史到 Redis List（右追加，保留最近 HISTORY_KEEP 条 + 刷新 TTL） */
    private void saveHistory(String key, String question, String answer) {
        try {
            String q = "用户：" + truncate(question, 500);
            String a = "墨墨：" + truncate(answer, 2000);
            redisTemplate.opsForList().rightPush(key, q);
            redisTemplate.opsForList().rightPush(key, a);
            redisTemplate.opsForList().trim(key, -HISTORY_KEEP, -1);
            redisTemplate.expire(key, HISTORY_TTL);
        } catch (Exception e) {
            log.warn("保存对话历史失败: {}", e.getMessage());
        }
    }

    private void send(SseEmitter emitter, Map<String, Object> payload) throws IOException {
        emitter.send(SseEmitter.event().data(OBJECT_MAPPER.writeValueAsString(payload), MediaType.APPLICATION_JSON));
    }

    private void sendQuietly(SseEmitter emitter, Map<String, Object> payload) {
        try {
            send(emitter, payload);
        } catch (IOException ignored) {
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}