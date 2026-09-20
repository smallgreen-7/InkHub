package com.example.InkHub_backend.service;

import com.example.InkHub_backend.service.AiSearchService.Hit;

import java.util.List;

/** Rerank 重排：对粗召回结果做精排，提升召回准确率 */
public interface RerankService {

    /**
     * 对粗召回的片段做 Rerank 精排
     *
     * @param query    用户问题
     * @param hits     粗召回结果
     * @param topK     精排后保留的数量
     * @return 按相关性排序的 topK 片段
     */
    List<Hit> rerank(String query, List<Hit> hits, int topK);
}
