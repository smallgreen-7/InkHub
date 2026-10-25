package com.example.InkHub_backend.vo;

import lombok.Data;

/** 查看 AI 知识库状态" */
@Data
public class AiStatsVO {
    /** 已发布文章数 */
    private long publishedArticles;
    /** 已索引文章数 */
    private long indexedArticles;
    /** 分块总数 */
    private long chunkCount;
    /** 最近一次索引时间 */
    private String lastIndexTime;
}
