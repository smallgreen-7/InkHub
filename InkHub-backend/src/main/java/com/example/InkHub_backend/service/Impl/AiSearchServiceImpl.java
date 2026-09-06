package com.example.InkHub_backend.service.Impl;

import com.example.InkHub_backend.service.AiSearchService;
import com.example.InkHub_backend.vo.ChatSourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 检索实现：问题向量化 → Qdrant similaritySearch → 转成业务对象
 * 相似度阈值在召回后代码里过滤（不依赖框架参数，兼容性最稳）
 */
@Service
@RequiredArgsConstructor
public class AiSearchServiceImpl implements AiSearchService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    @Override
    public List<Hit> searchChunks(String question, int topK, int maxPerArticle, double minScore) {
        // 多取再过滤（topK*3 保证过滤后够数）
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(topK * 3).build());
        List<Hit> hits = new ArrayList<>();
        for (Document doc : docs) {
            Double score = doc.getScore();
            if (score == null || score < minScore) {
                continue;
            }
            Object articleId = doc.getMetadata().get("articleId");
            Object title = doc.getMetadata().get("articleTitle");
            Object heading = doc.getMetadata().get("heading");
            if (articleId == null) continue;
            hits.add(new Hit(((Number) articleId).longValue(),
                    title == null ? "" : title.toString(),
                    heading == null ? "" : heading.toString(),
                    doc.getText(), score));
        }
        // 同文章限块 + 截断 topK
        List<Hit> picked = new ArrayList<>();
        Map<Long, Integer> perArticle = new LinkedHashMap<>();
        for (Hit hit : hits) {
            int used = perArticle.getOrDefault(hit.articleId(), 0);
            if (used >= maxPerArticle) continue;
            picked.add(hit);
            perArticle.put(hit.articleId(), used + 1);
            if (picked.size() >= topK) break;
        }
        return picked;
    }

    @Override
    public List<ChatSourceVO> searchArticles(String keyword, int topN) {
        List<Hit> hits = searchChunks(keyword, topN * 3, topN, 0.3);
        Map<Long, ChatSourceVO> map = new LinkedHashMap<>();
        for (Hit hit : hits) {
            ChatSourceVO vo = map.get(hit.articleId());
            if (vo == null) {
                vo = new ChatSourceVO();
                vo.setArticleId(hit.articleId());
                vo.setTitle(hit.articleTitle());
                vo.setScore(hit.score());
                map.put(hit.articleId(), vo);
            } else if (hit.score() > vo.getScore()) {
                vo.setScore(hit.score());
            }
        }
        return new ArrayList<>(map.values());
    }
}
