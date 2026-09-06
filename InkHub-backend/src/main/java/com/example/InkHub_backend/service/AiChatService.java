package com.example.InkHub_backend.service;

import com.example.InkHub_backend.dto.ChatRequest;
import com.example.InkHub_backend.dto.SummarizeRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** AI 助手：RAG 问答（流式）与摘要 */
public interface AiChatService {
    /** 问答：检索 → 拼提示词 → DeepSeek 流式 → SSE 推送（方法内负责 complete） */
    void chat(Long userId, ChatRequest req, SseEmitter emitter);

    /** AI 摘要（编辑器用） */
    String summarize(SummarizeRequest req);
}
