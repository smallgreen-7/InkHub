package com.example.InkHub_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** AI 检索参数（application.yaml 的 ai 段） */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    /** 检索返回最多片段数 */
    private int topK = 5;
    /** 相似度阈值（低于视为无关） */
    private double minScore = 0.35;
    /** 同文章最多命中块数（防某篇刷屏） */
    private int maxChunkPerArticle = 2;
    /** 单块目标长度（字符） */
    private int chunkSize = 600;
    /** 块重叠（字符） */
    private int chunkOverlap = 100;
    /** 每用户每分钟问答次数（Redis 限流） */
    private int rateLimit = 20;
    /** 语义搜索返回文章数 */
    private int searchTopN = 20;
}
