package com.example.InkHub_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Agent 对话请求体 */
@Data
public class AgentChatRequest {
    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题最长 500 字")
    private String question;

    /** 会话 id（前端生成 UUID，清空对话 = 换新 id；空则后端退化为单会话） */
    private String sessionId;
}