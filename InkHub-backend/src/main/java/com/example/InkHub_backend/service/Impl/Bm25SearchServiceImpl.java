package com.example.InkHub_backend.service.Impl;

import com.example.InkHub_backend.entity.Article;
import com.example.InkHub_backend.mapper.ArticleMapper;
import com.example.InkHub_backend.service.AiSearchService.Hit;
import com.example.InkHub_backend.service.Bm25SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * BM25 检索实现：MySQL FULLTEXT + ngram 分词器
 * 检索粒度是文章级，返回 Hit 时用 article.summary 或截断的 contentMd 作为 content
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Bm25SearchServiceImpl implements Bm25SearchService {

    private final ArticleMapper articleMapper;

    @Override
    public List<Hit> searchArticles(String query, int limit) {
        List<Article> articles = articleMapper.selectByFulltext(query, limit);
        List<Hit> hits = new ArrayList<>();
        for (Article a : articles) {
            String content = a.getSummary() != null && !a.getSummary().isBlank()
                    ? a.getSummary()
                    : truncate(a.getContentMd(), 600);
            hits.add(new Hit(a.getId(), a.getTitle(), "", content, 0.5));
        }
        return hits;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
