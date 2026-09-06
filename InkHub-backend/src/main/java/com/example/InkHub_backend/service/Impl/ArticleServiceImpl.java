package com.example.InkHub_backend.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.InkHub_backend.common.BusinessException;
import com.example.InkHub_backend.dto.ArticleDTO;
import com.example.InkHub_backend.dto.ArticleQueryDTO;
import com.example.InkHub_backend.entity.Article;
import com.example.InkHub_backend.entity.ArticleTag;
import com.example.InkHub_backend.mapper.ArticleMapper;
import com.example.InkHub_backend.mapper.ArticleTagMapper;
import com.example.InkHub_backend.service.AiIndexService;
import com.example.InkHub_backend.service.AiSearchService;
import com.example.InkHub_backend.service.ArticleService;
import com.example.InkHub_backend.utils.RedisKeys;
import com.example.InkHub_backend.vo.ArticleDetailVO;
import com.example.InkHub_backend.vo.ArticleVO;
import com.example.InkHub_backend.vo.ChatSourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AiIndexService aiIndexService;    // AI 索引联动
    private final AiSearchService aiSearchService;  // AI 语义搜索

    @Override
    public Page<ArticleVO> page(ArticleQueryDTO q) {
        int offset = (q.getPageNum() - 1) * q.getPageSize();
        List<ArticleVO> records;
        long total;
        if (q.getTagId() != null) {
            // 按标签：单独 SQL（中间表联查）
            records = articleMapper.selectPageByTag(q.getTagId(), offset, q.getPageSize());
            total = articleMapper.countByTag(q.getTagId());
        } else {
            records = articleMapper.selectArticlePage(q, offset, q.getPageSize());
            total = articleMapper.countArticlePage(q);
        }
        // 补标签名（每篇文章查一次，文章数少，够用；要优化可以用循环外批量查）
        records.forEach(this::fillTags);
        Page<ArticleVO> page = new Page<>(q.getPageNum(), q.getPageSize(), total);
        page.setRecords(records);
        return page;
    }

    @Override
    public ArticleDetailVO detail(Long id) {
        ArticleDetailVO vo = articleMapper.selectDetail(id);
        if (vo == null) {
            throw new BusinessException("文章不存在");
        }
        if (vo.getStatus() != 1) {
            throw new BusinessException("文章不存在");
        }
        // 浏览量：Redis INCR（不直接写库），返回值 = 本次 + 未落库增量
        Long redisInc = redisTemplate.opsForValue().increment(RedisKeys.articleView(vo.getId()));
        // 展示值 = 已落库(MySQL) + 未落库(Redis)，viewCount 是 Integer 可能为 null，先判空
        vo.setViewCount((vo.getViewCount() == null ? 0 : vo.getViewCount()) + redisInc.intValue());
        fillTags(vo);
        vo.setTagIds(articleTagMapper.selectTagIdsByArticle(vo.getId()));
        return vo;
    }

    @Override
    @Transactional   // 文章 + 标签中间表要一起成功/失败
    public Long create(Long userId, ArticleDTO dto) {
        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setContentMd(dto.getContentMd());
        article.setCover(dto.getCover());
        article.setCategoryId(dto.getCategoryId());
        article.setAuthorId(userId);
        int status = dto.getStatus() != null ? dto.getStatus() : 0;
        article.setStatus(status);
        article.setTop(0);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setFavoriteCount(0);
        article.setCommentCount(0);
        article.setPublishTime(status == 1 ? LocalDateTime.now() : null);   // 发布才填时间
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.insert(article);
        saveTags(article.getId(), dto.getTagIds());
        // AI 索引联动：发布成功且事务提交后，异步重建该文向量（异步线程读不到未提交数据，必须 afterCommit）
        int publishStatus = status;
        afterCommit(() -> {
            if (publishStatus == 1) {
                aiIndexService.indexArticleAsync(article.getId());
            }
        });
        return article.getId();
    }

    @Override
    @Transactional
    public void update(Long userId, Long articleId, ArticleDTO dto) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || !article.getAuthorId().equals(userId)) {
            throw new BusinessException(403, "只能编辑自己的文章");
        }
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setContentMd(dto.getContentMd());
        article.setCover(dto.getCover());
        article.setCategoryId(dto.getCategoryId());
        int status = dto.getStatus() != null ? dto.getStatus() : article.getStatus();
        // 状态机：草稿 → 发布（第一次发布填 publish_time）；已发布不改状态；下架不能自己恢复（走管理员）
        if (status == 1 && article.getStatus() != 1) {
            article.setStatus(1);
            article.setPublishTime(LocalDateTime.now());
        }
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.updateById(article);
        // 标签全量替换：先删后插
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId));
        saveTags(articleId, dto.getTagIds());
        // AI 索引联动：已发布文章内容变了，事务提交后异步重建
        afterCommit(() -> {
            if (article.getStatus() != null && article.getStatus() == 1) {
                aiIndexService.indexArticleAsync(articleId);
            }
        });
    }

    @Override
    @Transactional   // 删除文章 + 标签关联 + AI 分块移除要一起成功/失败（afterCommit 依赖事务存在）
    public void delete(Long userId, Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || !article.getAuthorId().equals(userId)) {
            throw new BusinessException(403, "只能删除自己的文章");
        }
        articleMapper.deleteById(articleId);
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId));
        // AI 索引联动：文章删除，向量点一并清掉
        afterCommit(() -> aiIndexService.removeArticle(articleId));
    }

    @Override
    public Page<ArticleVO> myArticles(Long userId, int pageNum, int pageSize) {
        // 我的文章：全部状态（含草稿），不联表也能看，但为了复用 VO 还是走联表
        Page<Article> p = articleMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getAuthorId, userId)
                        .orderByDesc(Article::getUpdateTime));
        List<ArticleVO> records = new ArrayList<>();
        p.getRecords().forEach(a -> {
            ArticleVO vo = new ArticleVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setSummary(a.getSummary());
            vo.setCover(a.getCover());
            vo.setCategoryId(a.getCategoryId());
            vo.setAuthorId(a.getAuthorId());
            vo.setStatus(a.getStatus());       // VO 里没这个字段？见下方补充说明
            vo.setTop(a.getTop());
            vo.setViewCount(a.getViewCount());
            vo.setLikeCount(a.getLikeCount());
            vo.setFavoriteCount(a.getFavoriteCount());
            vo.setCommentCount(a.getCommentCount());
            vo.setPublishTime(a.getPublishTime());
            fillTags(vo);
            records.add(vo);
        });
        Page<ArticleVO> page = new Page<>(pageNum, pageSize, p.getTotal());
        page.setRecords(records);
        return page;
    }

    // 私有方法：事务提交后执行的动作（AI 索引用：异步线程要读到已提交数据）
    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    // 私有方法：保存文章标签（去重）
    private void saveTags(Long articleId, List<Long> tagIds) {
        if (tagIds == null) return;
        tagIds.stream().distinct().forEach(tagId -> {
            ArticleTag at = new ArticleTag();
            at.setArticleId(articleId);
            at.setTagId(tagId);
            articleTagMapper.insert(at);
        });
    }

    // 私有方法：填充文章的标签名列表
    private void fillTags(ArticleVO vo) {
        List<String> tags = articleTagMapper.selectTagNamesByArticle(vo.getId());
        vo.setTags(tags);
    }
    @Override
    public List<ArticleVO> related(Long id) {
        ArticleDetailVO detail = articleMapper.selectDetail(id);
        if (detail == null || detail.getStatus() != 1) {
            throw new BusinessException("文章不存在");
        }
        List<ArticleVO> result = new ArrayList<>();
        // 1. 同分类推荐 5 条
        if (detail.getCategoryId() != null) {
            result = articleMapper.selectRelatedByCategory(detail.getCategoryId(), id, 5);
        }
        // 2. 不够 5 条，用同标签补
        if (result.size() < 5) {
            List<Long> tagIds = articleTagMapper.selectTagIdsByArticle(id);
            for (Long tagId : tagIds) {
                if (result.size() >= 5) break;
                for (ArticleVO vo : articleMapper.selectRelatedByTag(tagId, id, 5 - result.size())) {
                    boolean dup = result.stream().anyMatch(r -> r.getId().equals(vo.getId()));
                    if (!dup) result.add(vo);
                }
            }
        }
        return result;
    }

    // AI 语义搜索：向量召回相关文章 id → 取全量信息 → 按相关度排序返回
    @Override
    public List<ArticleVO> semanticSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        List<ChatSourceVO> related = aiSearchService.searchArticles(keyword, 20);
        if (related.isEmpty()) {
            return List.of();
        }
        List<Long> ids = related.stream().map(ChatSourceVO::getArticleId).toList();
        Map<Long, Article> byId = articleMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Article::getId, a -> a));
        List<ArticleVO> result = new ArrayList<>();
        for (ChatSourceVO source : related) {
            Article a = byId.get(source.getArticleId());
            if (a == null || a.getStatus() == null || a.getStatus() != 1) {
                continue;
            }
            ArticleVO vo = new ArticleVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setSummary(a.getSummary());
            vo.setCover(a.getCover());
            vo.setCategoryId(a.getCategoryId());
            vo.setAuthorId(a.getAuthorId());
            vo.setTop(a.getTop());
            vo.setViewCount(a.getViewCount());
            vo.setLikeCount(a.getLikeCount());
            vo.setFavoriteCount(a.getFavoriteCount());
            vo.setCommentCount(a.getCommentCount());
            vo.setPublishTime(a.getPublishTime());
            fillTags(vo);
            result.add(vo);
        }
        return result;
    }
}