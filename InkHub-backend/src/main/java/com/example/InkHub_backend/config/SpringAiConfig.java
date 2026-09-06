package com.example.InkHub_backend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 自定义 Bean
 * 多轮对话不走 ChatMemory/Advisor：由前端携带最近 6 轮 history，后端拼进提示词
 * （无状态、好扩展；需要会话持久化时再引入 MessageChatMemoryAdvisor + ChatMemoryRepository）
 */
@Configuration
public class SpringAiConfig {

    /** 全局默认 ChatClient：所有对话共用一个，统一带 system 基调 */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("你是墨墨，InkHub 知识社区的 AI 助手。"
                        + "可以正常寒暄和自我介绍；回答站内内容相关问题时，依据检索到的资料回答并标注引用，资料没有的绝不编造。")
                .build();
    }
}
