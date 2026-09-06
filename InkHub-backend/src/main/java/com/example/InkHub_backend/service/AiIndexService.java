package com.example.InkHub_backend.service;

import com.example.InkHub_backend.vo.AiStatsVO;

/** AI 索引生命周期：文章发布/编辑/删除时同步 Qdrant */
public interface AiIndexService {
    /** 同步索引单篇文章（仅 status=1；先清旧点再重建） */
    void indexArticle(Long articleId);

    /** 移除单篇文章的所有向量点 */
    void removeArticle(Long articleId);

    /** 异步索引（发布事务提交后调用） */
    void indexArticleAsync(Long articleId);

    /** 全量重建：清空 → 扫全部已发布文章 → 重建（管理端） */
    AiStatsVO rebuildAll();

    /** 统计 */
    AiStatsVO stats();
}
