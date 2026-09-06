package com.example.InkHub_backend.service;

import com.example.InkHub_backend.vo.ChatSourceVO;

import java.util.List;

/** 向量检索：给 RAG 问答和语义搜索共用 */
public interface AiSearchService {

    /** 命中片段 */
    record Hit(Long articleId, String articleTitle, String heading, String content, double score) {
    }

    /** 向量检索 topK 片段（RAG 用） */
    List<Hit> searchChunks(String question, int topK, int maxPerArticle, double minScore);

    /** 语义搜索：返回相关文章（含相似度） */
    List<ChatSourceVO> searchArticles(String keyword, int topN);
}
