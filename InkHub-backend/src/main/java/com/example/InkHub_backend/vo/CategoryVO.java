package com.example.InkHub_backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

// 分类 + 文章数
@Data
@Schema(description = "分类页（含文章数）")
public class CategoryVO {
    @Schema(description = "分类 id")
    private Long id;
    @Schema(description = "分类名")
    private String name;
    @Schema(description = "排序值（越小越靠前）")
    private Integer sort;
    @Schema(description = "该分类下已发布文章数")
    private Long articleCount;
}