package com.example.InkHub_backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 工具返回：文章统计信息（Function Calling 用） */
@Data
@AllArgsConstructor
public class ToolArticleStatsVO {
    private Long articleId;
    private String title;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
}
