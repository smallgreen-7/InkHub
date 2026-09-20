package com.example.InkHub_backend.service;

import com.example.InkHub_backend.service.AiSearchService.Hit;

import java.util.List;

/** 混合检索：向量检索 + BM25 关键词检索 + RRF 融合排序 */
public interface HybridSearchService {

    /**
     * 混合检索：两路召回 → RRF 融合 → 取 topK
     *
     * @param question     用户问题
     * @param topK         最终返回的片段数
     * @param maxPerArticle 同文章最多命中块数
     * @param minScore     向量检索最低相似度
     * @return 融合排序后的 topK 片段
     */
    List<Hit> hybridSearch(String question, int topK, int maxPerArticle, double minScore);
}
