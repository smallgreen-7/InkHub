package com.example.InkHub_backend.vo;

import lombok.Data;

/** AI 索引统计（管理端展示） */
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
