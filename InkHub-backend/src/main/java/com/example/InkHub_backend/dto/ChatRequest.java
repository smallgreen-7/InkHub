package com.example.InkHub_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** AI 问答请求体 */
@Data
public class ChatRequest {
    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题最长 500 字")
    private String question;

    /** 页面上下文：normal=全站 / article=针对某篇文章 */
    private String contextType;
    /** contextType=article 时的文章 id */
    private Long articleId;

    /** 最近几轮对话历史（[{role:user|assistant, content}]，最多带 6 轮） */
    private List<HistoryItem> history;

    @Data
    public static class HistoryItem {
        private String role;
        private String content;
    }
}
