package com.example.InkHub_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.InkHub_backend.common.BusinessException;
import com.example.InkHub_backend.common.R;
import com.example.InkHub_backend.entity.*;
import com.example.InkHub_backend.mapper.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// 管理端：SecurityConfig 里 /api/admin/** 已限定 hasRole("ADMIN")
// 注意：@Tag 注解与实体类 Tag 重名（entity.* 通配符导入），这里用全限定名，Tag 类名留给实体
@io.swagger.v3.oas.annotations.tags.Tag(name = "管理端接口", description = "文章/评论/分类/标签管理，全部需管理员角色（调试时在 Authorize 填管理员 token）")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final com.example.InkHub_backend.service.AiIndexService aiIndexService;   // AI 索引联动

    // ========== 文章管理 ==========

    // 全量文章列表（所有状态，可按状态过滤）
    @Operation(summary = "文章列表（管理）", description = "管理员接口；可按状态过滤：0草稿/1已发布/2已下架，不传查全部")
    @GetMapping("/articles")
    public R<List<Article>> articles(@Parameter(description = "文章状态：0草稿/1已发布/2已下架，可空") @RequestParam(required = false) Integer status) {
        return R.ok(articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(status != null, Article::getStatus, status)
                .orderByDesc(Article::getCreateTime)));
    }

    // 下架（发布 → 下架）
    @Operation(summary = "下架文章", description = "管理员接口；仅已发布文章可下架")
    @PutMapping("/articles/{id}/off")
    public R<Void> off(@Parameter(description = "文章 id") @PathVariable Long id) {
        Article article = mustExist(id);
        if (article.getStatus() != 1) {
            throw new BusinessException("只有已发布的文章才能下架");
        }
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .set(Article::getStatus, 2)
                .eq(Article::getId, id));
        aiIndexService.removeArticle(id);   // AI 联动：下架的文章不进检索
        return R.ok();
    }

    // 恢复发布（下架 → 发布，时间不变）
    @Operation(summary = "恢复发布", description = "管理员接口；仅已下架文章可恢复")
    @PutMapping("/articles/{id}/on")
    public R<Void> on(@Parameter(description = "文章 id") @PathVariable Long id) {
        Article article = mustExist(id);
        if (article.getStatus() != 2) {
            throw new BusinessException("只有已下架的文章才能恢复");
        }
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .set(Article::getStatus, 1)
                .eq(Article::getId, id));
        aiIndexService.indexArticleAsync(id);   // AI 联动：恢复发布重新纳入检索
        return R.ok();
    }

    // 置顶 / 取消置顶
    @Operation(summary = "置顶/取消置顶", description = "管理员接口；top=true 置顶，false 取消")
    @PutMapping("/articles/{id}/top")
    public R<Void> top(@Parameter(description = "文章 id") @PathVariable Long id,
                       @Parameter(description = "true 置顶 / false 取消") @RequestParam boolean top) {
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .set(Article::getTop, top ? 1 : 0)
                .eq(Article::getId, id));
        return R.ok();
    }

    // 删除任意文章（连同标签关联）
    @Operation(summary = "删除文章（管理）", description = "管理员接口；删除文章及其标签关联记录")
    @DeleteMapping("/articles/{id}")
    @Transactional
    public R<Void> delete(@Parameter(description = "文章 id") @PathVariable Long id) {
        articleMapper.deleteById(id);
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, id));
        aiIndexService.removeArticle(id);   // AI 联动：文章删除，向量点同事务移除
        return R.ok();
    }

    // ========== 评论管理 ==========

    // 全部评论（联表：文章标题 + 评论人昵称），管理页展示用
    @Operation(summary = "评论列表（管理）", description = "管理员接口；返回全部评论，含文章标题与评论人昵称")
    @GetMapping("/comments")
    public R<List<Map<String, Object>>> comments() {
        return R.ok(commentMapper.selectAllForAdmin());
    }

    // 删除任意评论
    @Operation(summary = "删除评论（管理）", description = "管理员接口；删除任意评论")
    @DeleteMapping("/comments/{id}")
    public R<Void> deleteComment(@Parameter(description = "评论 id") @PathVariable Long id) {
        commentMapper.deleteById(id);
        return R.ok();
    }

    // ========== 分类 / 标签维护 ==========

    @Operation(summary = "新增分类", description = "管理员接口；body 传 name、sort")
    @PostMapping("/categories")
    public R<Void> addCategory(@RequestBody Category category) {
        category.setId(null);
        category.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(category);
        return R.ok();
    }

    @Operation(summary = "编辑分类", description = "管理员接口；body 传 name、sort")
    @PutMapping("/categories/{id}")
    public R<Void> updateCategory(@Parameter(description = "分类 id") @PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryMapper.updateById(category);
        return R.ok();
    }

    @Operation(summary = "删除分类", description = "管理员接口；分类下的文章会变成无分类")
    @DeleteMapping("/categories/{id}")
    public R<Void> deleteCategory(@Parameter(description = "分类 id") @PathVariable Long id) {
        categoryMapper.deleteById(id);
        return R.ok();
    }

    @Operation(summary = "新增标签", description = "管理员接口；body 传 name")
    @PostMapping("/tags")
    public R<Void> addTag(@RequestBody Tag tag) {
        tag.setId(null);
        tag.setCreatedAt(LocalDateTime.now());
        tagMapper.insert(tag);
        return R.ok();
    }

    @Operation(summary = "删除标签", description = "管理员接口；同时清理文章-标签关联记录")
    @DeleteMapping("/tags/{id}")
    public R<Void> deleteTag(@Parameter(description = "标签 id") @PathVariable Long id) {
        tagMapper.deleteById(id);
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, id));
        return R.ok();
    }

    private Article mustExist(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        return article;
    }
}
