package com.example.InkHub_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

// 发布/编辑文章参数
@Data
@Schema(description = "文章发布/编辑参数")
public class ArticleDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长 100 字")
    @Schema(description = "标题", maxLength = 100)
    private String title;

    @Size(max = 255, message = "摘要最长 255 字")
    @Schema(description = "摘要（列表页展示，选填）", maxLength = 255)
    private String summary;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "Markdown 正文")
    private String contentMd;

    @Schema(description = "封面图 URL")
    private String cover;

    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "状态：0 存草稿 / 1 直接发布")
    private Integer status;

    @Schema(description = "标签 id 列表")
    private List<Long> tagIds;
}
