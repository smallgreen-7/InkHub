package com.example.InkHub_backend.service;

import com.example.InkHub_backend.dto.AgentChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Agent 对话：支持 Function Calling 的 AI 助手 */
public interface AgentChatService {

    /**
     * Agent 对话（SSE 流式）
     * LLM 自动判断是否调用工具（热门搜索/文章统计/作者搜索），基于工具结果组织回答
     */
    void chat(Long userId, AgentChatRequest req, SseEmitter emitter);
}
