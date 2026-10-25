package com.example.InkHub_backend.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.InkHub_backend.ai.MarkdownChunker;
import com.example.InkHub_backend.ai.MarkdownChunker.Chunk;
import com.example.InkHub_backend.entity.AiChunkRegistry;
import com.example.InkHub_backend.entity.Article;
import com.example.InkHub_backend.mapper.AiChunkRegistryMapper;
import com.example.InkHub_backend.mapper.ArticleMapper;
import com.example.InkHub_backend.service.AiIndexService;
import com.example.InkHub_backend.vo.AiStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 索引服务实现
 * 链路：content_md → MarkdownChunker 切块 → Document(带 metadata) → VectorStore.add
 *      （add 内部自动调 embedding 并写入 Qdrant）
 * 删除：按登记表 point_id 精确删除，避免依赖框架 filter API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiIndexServiceImpl implements AiIndexService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ArticleMapper articleMapper;
    private final AiChunkRegistryMapper registryMapper;
    private final MarkdownChunker chunker;
    private final VectorStore vectorStore;

    @Override
    public void indexArticle(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() == null || article.getStatus() != 1) {
            return;   // 非已发布不进索引
        }
        // 1. 切块（先不删旧索引：等新索引真正写成功再删，避免 embedding 失败导致文章彻底失去索引）
        List<Chunk> chunks = chunker.chunk(article.getContentMd());
        if (chunks.isEmpty()) {
            // 正文已清空，没有可检索内容，清掉旧索引
            removeArticle(articleId);
            return;
        }
        // 3. 组装 Document（metadata 存文章信息，检索命中直接带出）
        List<Document> docs = new ArrayList<>(chunks.size());
        List<AiChunkRegistry> registryList = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            // Qdrant 要求 point id 为 UUID：用确定性 UUID（同文章同块恒定），重建时天然幂等覆盖
            String pointId = UUID.nameUUIDFromBytes(
                    ("art:" + articleId + ":" + i).getBytes(StandardCharsets.UTF_8)).toString();
            Map<String, Object> meta = new HashMap<>();
            meta.put("articleId", articleId);
            meta.put("articleTitle", article.getTitle());
            meta.put("heading", chunk.heading());
            // 上下文增强：把文章标题 + 章节路径拼在块正文前面，让 embedding 编码到标题语义
            // （仅用于向量检索，展示时仍然用原始 chunk.content()）
            String enriched = buildEnrichedContent(article.getTitle(), chunk.heading(), chunk.content());
            docs.add(new Document(pointId, enriched, meta));
            AiChunkRegistry reg = new AiChunkRegistry();
            reg.setArticleId(articleId);
            reg.setPointId(pointId);
            reg.setCreateTime(LocalDateTime.now());
            registryList.add(reg);
        }
        // 4. 入库（自动 embedding）：pointId 是确定性 UUID，同一位置的新点直接覆盖旧点
        Set<String> newPointIds = new HashSet<>();
        for (Document doc : docs) {
            newPointIds.add(doc.getId());
        }
        List<String> written = new ArrayList<>(docs.size());
        int BATCH = 20;
        try {
            for (int i = 0; i < docs.size(); i += BATCH) {
                List<Document> batch = docs.subList(i, Math.min(i + BATCH, docs.size()));
                vectorStore.add(batch);
                for (Document doc : batch) {
                    written.add(doc.getId());
                }
            }
        } catch (RuntimeException e) {
            // 本次写入失败：清掉已写入的半成品点，旧索引保持可用
            deleteQuietly(written);
            throw e;
        }
        // 5. 新索引就绪后再清理旧点：只删新索引里不存在的（块数变少时避免旧块残留）
        List<AiChunkRegistry> oldRegs = registryMapper.selectList(
                new LambdaQueryWrapper<AiChunkRegistry>().eq(AiChunkRegistry::getArticleId, articleId));
        List<String> stale = new ArrayList<>();
        for (AiChunkRegistry reg : oldRegs) {
            if (!newPointIds.contains(reg.getPointId())) {
                stale.add(reg.getPointId());
            }
        }
        deleteQuietly(stale);
        // 6. 同步登记表：先清后插，保证和 Qdrant 里的点一致
        registryMapper.delete(new LambdaQueryWrapper<AiChunkRegistry>()
                .eq(AiChunkRegistry::getArticleId, articleId));
        registryList.forEach(registryMapper::insert);
        log.info("AI 索引完成 articleId={} chunks={} 清理旧点={}", articleId, chunks.size(), stale.size());
    }

    @Override
    public void removeArticle(Long articleId) {
        // 按登记表拿到点 id 删除 Qdrant，再清登记
        List<AiChunkRegistry> regs = registryMapper.selectList(
                new LambdaQueryWrapper<AiChunkRegistry>().eq(AiChunkRegistry::getArticleId, articleId));
        deleteQuietly(regs.stream().map(AiChunkRegistry::getPointId).toList());
        registryMapper.delete(new LambdaQueryWrapper<AiChunkRegistry>()
                .eq(AiChunkRegistry::getArticleId, articleId));
    }

    @Override
    @Async("aiIndexExecutor")
    public void indexArticleAsync(Long articleId) {
        try {
            indexArticle(articleId);
        } catch (Exception e) {
            // 索引失败不影响发布主流程，管理端可全量重建兜底
            log.error("AI 索引失败 articleId={}: {}", articleId, e.getMessage(), e);
        }
    }

    /**
     * 尽力删除向量点：失败只记日志不抛异常。
     * 清理旧点/半成品点属于辅助动作，不能因为它失败把主流程（发布、下架）带崩。
     */
    private void deleteQuietly(List<String> pointIds) {
        if (pointIds == null || pointIds.isEmpty()) {
            return;   // Qdrant 不接受空列表删除
        }
        try {
            vectorStore.delete(pointIds);
        } catch (Exception e) {
            log.warn("删除向量点失败 pointIds={}: {}", pointIds, e.getMessage());
        }
    }

    @Override
    public AiStatsVO rebuildAll() {
        // 1. 按登记表收集全部旧点并删除（Qdrant 不支持空列表删除，走精确删除）
        List<AiChunkRegistry> all = registryMapper.selectList(null);
        if (!all.isEmpty()) {
            vectorStore.delete(all.stream().map(AiChunkRegistry::getPointId).toList());
        }
        registryMapper.delete(null);
        // 2. 扫全部已发布文章逐篇重建
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1).orderByAsc(Article::getId));
        int ok = 0;
        for (Article article : articles) {
            try {
                indexArticle(article.getId());
                ok++;
            } catch (Exception e) {
                log.error("重建失败 articleId={}: {}", article.getId(), e.getMessage());
            }
        }
        log.info("AI 索引全量重建完成: {}/{}", ok, articles.size());
        return stats();
    }

    @Override
    public AiStatsVO stats() {
        AiStatsVO vo = new AiStatsVO();
        vo.setPublishedArticles(articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)));
        Long chunkCount = registryMapper.selectCount(null);
        vo.setChunkCount(chunkCount == null ? 0 : chunkCount);
        List<Object> ids = registryMapper.selectObjs(new LambdaQueryWrapper<AiChunkRegistry>()
                .select(AiChunkRegistry::getArticleId).groupBy(AiChunkRegistry::getArticleId));
        vo.setIndexedArticles(ids == null ? 0 : ids.size());
        AiChunkRegistry latest = registryMapper.selectOne(new LambdaQueryWrapper<AiChunkRegistry>()
                .orderByDesc(AiChunkRegistry::getCreateTime).last("LIMIT 1"));
        vo.setLastIndexTime(latest == null || latest.getCreateTime() == null
                ? "" : latest.getCreateTime().format(FMT));
        return vo;
    }

    /**
     * 构造增强版块正文：文章标题 + 章节路径 + 原始内容
     * 仅用于向量 embedding（让模型把标题语义编码进去），
     * 检索结果展示时仍然用 metadata 里的 heading + 原始 content。
     */
    private String buildEnrichedContent(String title, String heading, String content) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isBlank()) {
            sb.append("文章：").append(title).append("\n");
        }
        if (heading != null && !heading.isBlank()) {
            sb.append("章节：").append(heading).append("\n\n");
        }
        sb.append(content);
        return sb.toString();
    }
}
