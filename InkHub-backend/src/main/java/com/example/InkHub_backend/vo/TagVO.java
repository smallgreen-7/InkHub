package com.example.InkHub_backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "标签页（含文章数）")
public class TagVO {
    @Schema(description = "标签 id")
    private Long id;
    @Schema(description = "标签名")
    private String name;
    @Schema(description = "该标签下文章数")
    private Long articleCount;   // 该标签下文章数
}