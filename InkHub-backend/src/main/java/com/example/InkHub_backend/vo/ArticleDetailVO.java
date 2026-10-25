package com.example.InkHub_backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

// 文章详情页
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文章详情页（继承列表页所有字段）")
public class ArticleDetailVO extends ArticleVO {
    @Schema(description = "Markdown 全文")
    private String contentMd;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "标签 id ")
    private List<Long> tagIds;
}