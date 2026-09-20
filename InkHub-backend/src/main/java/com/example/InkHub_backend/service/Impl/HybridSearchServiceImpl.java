package com.example.InkHub_backend.service.Impl;

import com.example.InkHub_backend.service.AiSearchService;
import com.example.InkHub_backend.service.AiSearchService.Hit;
import com.example.InkHub_backend.service.Bm25SearchService;
import com.example.InkHub_backend.service.HybridSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchServiceImpl implements HybridSearchService {

    private static final int RRF_K = 60;

    private final AiSearchService aiSearchService;
    private final Bm25SearchService bm25SearchService;

    @Override
    public List<Hit> hybridSearch(String question, int topK, int maxPerArticle, double minScore) {
        // 1. 两路召回（topK 就是候选数，上层已传 topK*3；这里不再乘）
        List<Hit> vectorHits = aiSearchService.searchChunks(question, topK, maxPerArticle, minScore);
        List<Hit> bm25Hits = bm25SearchService.searchArticles(question, topK);

        // 2. RRF：按文章 ID 聚合排名分
        Map<Long, Double> rrfScores = new HashMap<>();
        for (int i = 0; i < vectorHits.size(); i++) {
            rrfScores.merge(vectorHits.get(i).articleId(), 1.0 / (RRF_K + i + 1), Double::sum);
        }
        for (int i = 0; i < bm25Hits.size(); i++) {
            rrfScores.merge(bm25Hits.get(i).articleId(), 1.0 / (RRF_K + i + 1), Double::sum);
        }

        // 3. 每篇文章选最佳代表块（向量命中优先取最高分，否则用 BM25 的）
        Map<Long, Hit> bestBlockByArticle = new HashMap<>();
        // 3a. 先放向量块：同篇取相似度最高的（后面的 merge 用 score 比较）
        for (Hit hit : vectorHits) {
            Hit existing = bestBlockByArticle.get(hit.articleId());
            if (existing == null || hit.score() > existing.score()) {
                bestBlockByArticle.put(hit.articleId(), hit);
            }
        }
        // 3b. 向量没覆盖到的文章，用 BM25 的内容兜底
        for (Hit hit : bm25Hits) {
            bestBlockByArticle.putIfAbsent(hit.articleId(), hit);
        }

        // 4. 按 RRF 分降序取 topK 篇
        List<Long> sortedIds = rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 5. 输出每篇的代表块
        List<Hit> result = new ArrayList<>();
        for (Long articleId : sortedIds) {
            Hit block = bestBlockByArticle.get(articleId);
            if (block != null) {
                result.add(block);
            }
        }

        log.info("Hybrid search: vector={}块, bm25={}篇, 融合输出={}篇", vectorHits.size(), bm25Hits.size(), result.size());
        return result;
    }
}