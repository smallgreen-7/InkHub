package com.example.InkHub_backend.config;

import com.example.InkHub_backend.ai.tools.InkHubTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置
 *
 * 两个 ChatClient：
 * - chatClient：普通 RAG 问答用（混合检索 → Rerank → 防幻觉）
 * - agentChatClient：Agent 对话用（注册站内工具，支持 Function Calling）
 */
@Configuration
public class SpringAiConfig {

    /** 普通 RAG 问答 ChatClient（无工具） */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("你是墨墨，InkHub 知识社区的 AI 助手。"
                        + "可以正常寒暄和自我介绍；回答站内内容相关问题时，依据检索到的资料回答并标注引用，资料没有的绝不编造。")
                .build();
    }

    /**
     * Agent 专用 ChatClient（注册 Function Calling 工具）
     * defaultTools() 直接传对象，Spring AI 自动扫描 @Tool 注解的方法
     */
    @Bean("agentChatClient")
    public ChatClient agentChatClient(ChatClient.Builder builder, InkHubTools tools) {
        return builder
                .defaultSystem("你是墨墨，InkHub 知识社区的 AI 助手。"
                        + "你可以调用站内工具获取文章和用户信息。"
                        + "当用户问'最近有什么热门文章'时调用 searchHotArticles 工具；"
                        + "当用户问某篇文章的浏览量/点赞数时调用 getArticleStats 工具；"
                        + "当用户搜索作者时调用 searchUsers 工具。"
                        + "拿到工具返回的结果后，用自然语言组织回答，不要只返回 JSON。")
                .defaultTools(tools)
                .build();
    }
}
