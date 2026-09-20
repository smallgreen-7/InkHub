package com.example.InkHub_backend.service;

import com.example.InkHub_backend.service.AiSearchService.Hit;

import java.util.List;

/** BM25 关键词检索：用 MySQL FULLTEXT 索引做全文检索 */
public interface Bm25SearchService {

    /** BM25 检索文章（返回和向量检索相同的 Hit 格式，方便融合） */
    List<Hit> searchArticles(String query, int limit);
}
