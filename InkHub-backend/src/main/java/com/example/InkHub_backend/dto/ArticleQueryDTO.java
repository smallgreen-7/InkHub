package com.example.InkHub_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

// 文章流查询参数：分类/标签/搜索/排序/分页
@Data
@Schema(description = "文章列表查询参数")
public class ArticleQueryDTO {
    @Schema(description = "页码，从 1 开始")
    private Integer pageNum = 1;
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
    @Schema(description = "分类 id")
    private Long categoryId;
    @Schema(description = "标签 id")
    private Long tagId;
    @Schema(description = "搜索关键词")
    private String keyword;
    @Schema(description = "排序：latest 最新 / hot 最热")
    private String sort = "latest";
}