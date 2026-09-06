package com.example.InkHub_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.InkHub_backend.common.BusinessException;
import com.example.InkHub_backend.common.R;
import com.example.InkHub_backend.dto.ArticleDTO;
import com.example.InkHub_backend.dto.ArticleQueryDTO;
import com.example.InkHub_backend.service.ArticleService;
import com.example.InkHub_backend.vo.ArticleDetailVO;
import com.example.InkHub_backend.vo.ArticleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "文章接口", description = "文章流的查询、发布、编辑、删除（写操作需登录）")
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        throw new BusinessException(401, "未登录");
    }

    // 文章流（公开）
    @Operation(summary = "分页查询文章列表", description = "公开接口；支持关键词搜索、分类/标签筛选、最新/最热排序")
    @GetMapping
    public R<Page<ArticleVO>> page(ArticleQueryDTO q) {
        return R.ok(articleService.page(q));
    }

    // 详情（公开，浏览量 +1）
    @Operation(summary = "文章详情", description = "公开接口；浏览量 +1，返回 Markdown 全文与标签 id")
    @GetMapping("/{id}")
    public R<ArticleDetailVO> detail(@Parameter(description = "文章 id") @PathVariable Long id) {
        return R.ok(articleService.detail(id));
    }

    // 发布 / 存草稿（登录）
    @Operation(summary = "发布文章/存草稿", description = "需登录；status=1 直接发布，status=0 存草稿；返回文章 id")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ArticleDTO dto) {
        return R.ok(articleService.create(currentUserId(), dto));
    }

    // 编辑（登录，作者本人）
    @Operation(summary = "编辑文章", description = "需登录，仅作者本人可操作")
    @PutMapping("/{id}")
    public R<Void> update(@Parameter(description = "文章 id") @PathVariable Long id, @Valid @RequestBody ArticleDTO dto) {
        articleService.update(currentUserId(), id, dto);
        return R.ok();
    }

    // 删除（登录，作者本人）
    @Operation(summary = "删除文章", description = "需登录，仅作者本人可操作")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "文章 id") @PathVariable Long id) {
        articleService.delete(currentUserId(), id);
        return R.ok();
    }

    // 我的文章（登录）
    @Operation(summary = "我的文章", description = "需登录；分页返回当前用户的文章（含草稿）")
    @GetMapping("/mine")
    public R<Page<ArticleVO>> mine(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int pageNum,
                                   @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(articleService.myArticles(currentUserId(), pageNum, pageSize));
    }
    // 相关文章推荐（公开）
    @Operation(summary = "相关文章推荐", description = "同分类优先、同标签补充，最多 5 条")
    @GetMapping("/{id}/related")
    public R<List<ArticleVO>> related(
            @Parameter(description = "文章 id") @PathVariable Long id) {
        return R.ok(articleService.related(id));
    }

    // AI 语义搜索（公开）：向量召回语义相关的文章，按相关度返回
    @Operation(summary = "语义搜索", description = "AI 向量检索，找语义相关的文章（不依赖关键词完全匹配）")
    @GetMapping("/semantic")
    public R<List<ArticleVO>> semanticSearch(
            @Parameter(description = "搜索词") @RequestParam String keyword) {
        return R.ok(articleService.semanticSearch(keyword));
    }
}