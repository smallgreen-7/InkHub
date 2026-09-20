package com.example.InkHub_backend.controller;

import com.example.InkHub_backend.common.BusinessException;
import com.example.InkHub_backend.common.R;
import com.example.InkHub_backend.dto.AgentChatRequest;
import com.example.InkHub_backend.dto.ChatRequest;
import com.example.InkHub_backend.dto.SummarizeRequest;
import com.example.InkHub_backend.service.AiChatService;
import com.example.InkHub_backend.service.AgentChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 接口：SecurityConfig 未放行 /api/ai/**，默认 anyRequest().authenticated()（要登录）
 * SSE 事件：{"type":"delta"|"sources"|"done"|"error", ...}
 */
@Tag(name = "AI 助手接口")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiChatService aiChatService;
    private final AgentChatService agentChatService;

    @Operation(summary = "AI 问答", description = "SSE 流式，基于站内文章回答")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(180_000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> emitter.complete());
        aiChatService.chat(currentUserId(), req, emitter);   // @Async，立即返回
        return emitter;
    }

    @Operation(summary = "AI 生成摘要")
    @PostMapping("/summarize")
    public R<String> summarize(@Valid @RequestBody SummarizeRequest req) {
        return R.ok(aiChatService.summarize(req));
    }

    @Operation(summary = "Agent 对话", description = "SSE 流式，支持 Function Calling 调用站内工具")
    @PostMapping(value = "/agent/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter agentChat(@Valid @RequestBody AgentChatRequest req) {
        SseEmitter emitter = new SseEmitter(180_000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> emitter.complete());
        agentChatService.chat(currentUserId(), req, emitter);
        return emitter;
    }

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        throw new BusinessException(401, "未登录");
    }
}
