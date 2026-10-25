package com.example.InkHub_backend.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.InkHub_backend.common.BusinessException;
import com.example.InkHub_backend.common.R;
import com.example.InkHub_backend.entity.Article;
import com.example.InkHub_backend.entity.Comment;
import com.example.InkHub_backend.entity.Notification;
import com.example.InkHub_backend.mapper.ArticleMapper;
import com.example.InkHub_backend.mapper.CommentMapper;
import com.example.InkHub_backend.mapper.NotificationMapper;
import com.example.InkHub_backend.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "评论接口", description = "文章评论的查询、发表、回复、删除")
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final NotificationMapper notificationMapper;

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        throw new BusinessException(401, "未登录");
    }

    // 查对应文章的评论列表
    @Operation(summary = "评论列表", description = "公开接口；返回某文章全部评论（含二级回复，parentId 区分）")
    @GetMapping
    public R<List<CommentVO>> list(@Parameter(description = "文章 id") @RequestParam Long articleId) {
        return R.ok(commentMapper.selectByArticle(articleId));
    }

    // 发表评论 / 回复（登录；回复时传 parentId）
    @Operation(summary = "发表评论/回复", description = "需登录；回复他人时传 parentId（被回复评论的 id）；body 传评论内容字符串")
    @PostMapping
    @Transactional
    public R<Long> add(@Parameter(description = "文章 id") @RequestParam Long articleId,
                       @Parameter(description = "被回复的评论 id，一级评论不传") @RequestParam(required = false) Long parentId,
                       @RequestBody String content) {
        // 校验文章存在且已发布
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() != 1) {
            throw new BusinessException("文章不存在");
        }
        if (content == null || content.trim().isEmpty() || content.trim().length() > 500) {
            throw new BusinessException(400, "评论内容 1-500 字");
        }
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(currentUserId());
        comment.setParentId(parentId);
        comment.setContent(content.trim());
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);

        // 回复时给被回复人发通知（自己回复自己不通知）
        if (parentId != null) {
            Comment parent = commentMapper.selectById(parentId);
            Long myId = currentUserId();
            if (parent != null && !parent.getUserId().equals(myId)) {
                Long receiverId = parent.getUserId();
                Notification n = new Notification();
                n.setUserId(receiverId);
                n.setFromUserId(myId);
                n.setType(1);   // 1 评论回复
                n.setContent("回复了你的评论：" + content.trim());
                n.setArticleId(articleId);
                n.setCommentId(comment.getId());
                n.setIsRead(0);
                n.setCreateTime(LocalDateTime.now());
                notificationMapper.insert(n);
            }
        }

        // 文章评论数 +1（原子更新，不查再改）
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .setSql("comment_count = comment_count + 1")
                .eq(Article::getId, articleId));
        return R.ok(comment.getId());
    }

    // 删除评论（作者本人或管理员；管理员判断在 Admin 接口里做）
    @Operation(summary = "删除评论", description = "需登录，仅作者本人可删除自己的评论")
    @DeleteMapping("/{id}")
    @Transactional
    public R<Void> delete(@Parameter(description = "评论 id") @PathVariable Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getUserId().equals(currentUserId())) {
            throw new BusinessException(403, "只能删除自己的评论");
        }
        commentMapper.deleteById(id);
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .setSql("comment_count = GREATEST(comment_count - 1, 0)")
                .eq(Article::getId, comment.getArticleId()));
        return R.ok();
    }
}
