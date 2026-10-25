package com.example.InkHub_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.InkHub_backend.common.BusinessException;
import com.example.InkHub_backend.common.R;
import com.example.InkHub_backend.entity.Article;
import com.example.InkHub_backend.entity.Favorite;
import com.example.InkHub_backend.entity.Notification;
import com.example.InkHub_backend.mapper.ArticleMapper;
import com.example.InkHub_backend.mapper.FavoriteMapper;
import com.example.InkHub_backend.mapper.NotificationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// 收藏：收藏/取消/状态（逻辑与点赞一致）
@Tag(name = "收藏接口", description = "文章的收藏/取消收藏/收藏状态/我的收藏列表（全部需登录）")
@RestController
@RequestMapping("/api/articles/{articleId}/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteMapper favoriteMapper;
    private final ArticleMapper articleMapper;
    private final NotificationMapper notificationMapper;

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        throw new BusinessException(401, "未登录");
    }

    // 收藏（幂等：已收藏就什么都不做，唯一索引兜底并发）
    @Operation(summary = "收藏文章", description = "需登录；幂等，重复收藏不会重复计数")
    @PostMapping
    @Transactional
    public R<Void> favorite(@Parameter(description = "文章 id") @PathVariable Long articleId) {
        Long userId = currentUserId();
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getArticleId, articleId)
                        .eq(Favorite::getUserId, userId));
        if (count == 0) {
            Favorite favorite = new Favorite();
            favorite.setArticleId(articleId);
            favorite.setUserId(userId);
            favorite.setCreateTime(LocalDateTime.now());
            try {
                favoriteMapper.insert(favorite);
            } catch (Exception e) {
                // 并发下唯一索引冲突：说明别人刚收藏过
                return R.ok();
            }
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .setSql("favorite_count = favorite_count + 1")
                    .eq(Article::getId, articleId));

            // 收藏通知（幂等：只在第一次收藏时发）
            Article article = articleMapper.selectById(articleId);
            if (article != null && !article.getAuthorId().equals(userId)) {
                Notification n = new Notification();
                n.setUserId(article.getAuthorId());
                n.setFromUserId(userId);
                n.setType(3);   // 3 收藏
                n.setContent("收藏了你的文章《" + article.getTitle() + "》");
                n.setArticleId(articleId);
                n.setIsRead(0);
                n.setCreateTime(LocalDateTime.now());
                notificationMapper.insert(n);
            }
        }
        return R.ok();
    }

    // 取消收藏（幂等）
    @Operation(summary = "取消收藏", description = "需登录；幂等，未收藏时调用无影响")
    @DeleteMapping
    @Transactional
    public R<Void> unfavorite(@Parameter(description = "文章 id") @PathVariable Long articleId) {
        Long userId = currentUserId();
        int deleted = favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getArticleId, articleId)
                .eq(Favorite::getUserId, userId));
        if (deleted > 0) {
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .setSql("favorite_count = GREATEST(favorite_count - 1, 0)")
                    .eq(Article::getId, articleId));
        }
        return R.ok();
    }

    // 我是否收藏过（详情页回显）
    @Operation(summary = "收藏状态", description = "需登录；返回当前用户是否已收藏")
    @GetMapping("/status")
    public R<Boolean> status(@Parameter(description = "文章 id") @PathVariable Long articleId) {
        Long userId = currentUserId();
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getArticleId, articleId)
                .eq(Favorite::getUserId, userId));
        return R.ok(count > 0);
    }

    // 我的收藏列表（个人中心用；路由写全路径，保持 GET /api/user/favorites 与前端一致）
    @Operation(summary = "我的收藏列表", description = "需登录；返回当前用户收藏的文章（标题+收藏时间），按收藏时间倒序")
    @GetMapping("/api/user/favorites")
    public R<List<Map<String, Object>>> myFavorites() {
        Long userId = currentUserId();
        return R.ok(favoriteMapper.selectMyFavorites(userId));
    }
}