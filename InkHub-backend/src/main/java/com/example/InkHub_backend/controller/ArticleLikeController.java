package com.example.InkHub_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.InkHub_backend.common.BusinessException;
import com.example.InkHub_backend.common.R;
import com.example.InkHub_backend.entity.Article;
import com.example.InkHub_backend.entity.ArticleLike;
import com.example.InkHub_backend.entity.Notification;
import com.example.InkHub_backend.mapper.ArticleLikeMapper;
import com.example.InkHub_backend.mapper.ArticleMapper;
import com.example.InkHub_backend.mapper.NotificationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

// 点赞：点赞/取消/状态。
@Tag(name = "点赞接口", description = "文章的点赞/取消点赞/点赞状态（全部需登录）")
@RestController
@RequestMapping("/api/articles/{articleId}/like")
@RequiredArgsConstructor
public class ArticleLikeController {

    private final ArticleLikeMapper likeMapper;
    private final ArticleMapper articleMapper;
    private final NotificationMapper notificationMapper;

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        throw new BusinessException(401, "未登录");
    }

    // 点赞（幂等：已点过就什么都不做，靠唯一索引兜底）
    @Operation(summary = "点赞文章", description = "需登录；幂等，重复点赞不会重复计数")
    @PostMapping
    @Transactional
    public R<Void> like(@Parameter(description = "文章 id") @PathVariable Long articleId) {
        Long userId = currentUserId();
        Long count = likeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, articleId)
                .eq(ArticleLike::getUserId, userId));
        if (count == 0) {
            ArticleLike like = new ArticleLike();
            like.setArticleId(articleId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            try {
                likeMapper.insert(like);
            } catch (DuplicateKeyException e) {
                // 并发下唯一索引冲突：说明别人刚点过，直接忽略
                return R.ok();
            }
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .setSql("like_count = like_count + 1")
                    .eq(Article::getId, articleId));

            // 点赞通知（幂等：只在第一次点赞时发）
            Article article = articleMapper.selectById(articleId);
            if (article != null && !article.getAuthorId().equals(userId)) {
                Notification n = new Notification();
                n.setUserId(article.getAuthorId());
                n.setFromUserId(userId);
                n.setType(2);   // 2 点赞
                n.setContent("点赞了你的文章《" + article.getTitle() + "》");
                n.setArticleId(articleId);
                n.setIsRead(0);
                n.setCreateTime(LocalDateTime.now());
                notificationMapper.insert(n);
            }
        }
        return R.ok();
    }

    // 取消点赞（幂等）
    @Operation(summary = "取消点赞", description = "需登录；幂等，未点赞时调用无影响")
    @DeleteMapping
    @Transactional
    public R<Void> unlike(@Parameter(description = "文章 id") @PathVariable Long articleId) {
        Long userId = currentUserId();
        int deleted = likeMapper.delete(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, articleId)
                .eq(ArticleLike::getUserId, userId));
        if (deleted > 0) {
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .setSql("like_count = GREATEST(like_count - 1, 0)")
                    .eq(Article::getId, articleId));
        }
        return R.ok();
    }

    // 我是否点过赞（详情页回显）
    @Operation(summary = "点赞状态", description = "需登录；返回当前用户是否已点赞")
    @GetMapping("/status")
    public R<Boolean> status(@Parameter(description = "文章 id") @PathVariable Long articleId) {
        Long userId = currentUserId();
        Long count = likeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, articleId)
                .eq(ArticleLike::getUserId, userId));
        return R.ok(count > 0);
    }
}