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

    /** 混合检索 RRF 融合权重：向量路权重（强命中优先，避免被 BM25 噪声稀释） */
    private double vectorWeight = 1.0;

    /** 混合检索 RRF 融合权重：BM25 路权重（中文 ngram 召回过宽，降权） */
    private double bm25Weight = 0.3;

    /** BM25 路最多取多少篇候选（防止一次召回近全库） */
    private int bm25TopN = 5;


    private String rerankModel = "qwen3-rerank";

    private String rerankBaseUrl = "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank";

    private String rerankApiKey = "";
}
