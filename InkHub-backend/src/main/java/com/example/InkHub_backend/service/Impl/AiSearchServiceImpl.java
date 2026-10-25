package com.example.InkHub_backend.service.Impl;

import com.example.InkHub_backend.mapper.ArticleMapper;
import com.example.InkHub_backend.service.AiSearchService;
import com.example.InkHub_backend.vo.ArticleVO;
import com.example.InkHub_backend.vo.ChatSourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 检索实现：问题向量化 → Qdrant similaritySearch → 转成业务对象
 * 相似度阈值在召回后代码里过滤（不依赖框架参数，兼容性最稳）
 */
@Service
@RequiredArgsConstructor
public class AiSearchServiceImpl implements AiSearchService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final ArticleMapper articleMapper;

    //去向量数据库检索文本块，顺便过滤多余的文本块
    @Override
    public List<Hit> searchChunks(String question, int topK, int maxPerArticle, double minScore) {
        // 多取再过滤（topK*3 保证过滤后够数）
        List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(topK * 3).build());
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
            long articleIdVal;
            if (articleId instanceof Number num) {
                articleIdVal = num.longValue();
            } else {
                articleIdVal = Long.parseLong(articleId.toString().trim());
            }
            hits.add(new Hit(articleIdVal,
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

    //根据查到文本块聚合对应相关文章
    @Override
    public List<ChatSourceVO> searchArticles(String keyword, int topN) {
        List<Hit> hits = searchChunks(keyword, topN * 3, topN, 0.3);
        //收集所有article（去重）
        List<Long> ids = hits.stream()
                .map(Hit::articleId)
                .distinct()
                .limit(topN)
                .toList();
        // 空列表不能直接回表：Mapper 里是 foreach 拼 IN(...)，空集合会生成 IN () 导致 SQL 语法错误
        if (ids.isEmpty()) {
            return List.of();
        }
        //批量IN回表，一条SQL就查完
        List<ArticleVO> articles = articleMapper.selectVOByIds(ids);
        Map<Long, ArticleVO> articleMap = articles.stream()
                .collect(Collectors.toMap(ArticleVO::getId, a -> a));
        //按召回顺序去重
        List<ChatSourceVO> chatSources = new ArrayList<>();
        Set<Long> added = new HashSet<>();  //去重（一篇文章存一个
        for (Hit hit : hits) {
            if(added.contains(hit.articleId())) continue;
            ArticleVO avo = articleMap.get(hit.articleId());
            if (avo == null) continue;
            ChatSourceVO cvo = new ChatSourceVO();
            cvo.setArticleId(hit.articleId());
            cvo.setTitle(avo.getTitle());
            cvo.setSummary(avo.getSummary());
            cvo.setAuthorname(avo.getAuthorName());
            cvo.setScore(hit.score());
            chatSources.add(cvo);
            added.add(hit.articleId());
            if(chatSources.size() >= topN) break;
        }
        return chatSources;
    }
}
