package com.example.InkHub_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/** 异步线程池：AI 索引专用（embedding 调外部 API 慢，不占业务线程） */
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean("aiIndexExecutor")
    public Executor aiIndexExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-index-");
        executor.initialize();
        return executor;
    }
}
