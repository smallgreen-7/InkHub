package com.example.InkHub_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private int topK = 5;

    private double minScore = 0.35;

    private int maxChunkPerArticle = 2;

    private int chunkSize = 600;

    private int chunkOverlap = 100;

    private int rateLimit = 20;

    private int searchTopN = 20;


    private String rerankModel = "qwen3-rerank";

    private String rerankBaseUrl = "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank";

    private String rerankApiKey = "";
}
