package com.example.InkHub_backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

// 文章列表：文章 + 作者昵称 + 分类名 + 标签
@Data
@Schema(description = "文章列表页")
public class ArticleVO {
    @Schema(description = "文章 id")
    private Long id;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "摘要")
    private String summary;
    @Schema(description = "封面图 URL")
    private String cover;
    @Schema(description = "分类 id")
    private Long categoryId;
    @Schema(description = "分类名")
    private String categoryName;
    @Schema(description = "作者 id")
    private Long authorId;
    @Schema(description = "作者昵称")
    private String authorName;
    @Schema(description = "是否置顶：1 是 / 0 否")
    private Integer top;
    @Schema(description = "浏览量")
    private Integer viewCount;
    @Schema(description = "点赞数")
    private Integer likeCount;
    @Schema(description = "收藏数")
    private Integer favoriteCount;
    @Schema(description = "评论数")
    private Integer commentCount;
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
    @Schema(description = "标签名列表")
    private List<String> tags;
    @Schema(description = "状态：0草稿/1已发布/2已下架")
    private Integer status;
}