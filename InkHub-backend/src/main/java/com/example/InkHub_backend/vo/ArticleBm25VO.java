package com.example.InkHub_backend.vo;

import lombok.Data;

/**
 * BM25 全文检索结果：文章基本信息 + MySQL 全文相关度分数
 * score 范围：0 ~ 正无穷，通常匹配度越高分数越大，
 * 不同查询之间的分数不可直接比较（因为分母随查询词频变化）。
 */
@Data
public class ArticleBm25VO {
    private Long id;
    private String title;
    private String summary;
    private String contentMd;
    /** MySQL MATCH...AGAINST 返回的相关度分数 */
    private Double score;
}
