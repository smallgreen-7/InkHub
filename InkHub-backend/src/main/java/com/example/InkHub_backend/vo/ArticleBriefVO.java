package com.example.InkHub_backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 工具返回：文章简要信息，给Function Calling 用的） */
@Data
@AllArgsConstructor
public class ArticleBriefVO {
    /**文章ID */
    private Long id;
    /** 文章标题 */
    private String title;
    /** 摘要 */
    private String summary;
    /** 作者名字 */
    private String authorName;
    /** 浏览量 */
    private Integer viewCount;
    /** 点赞数 */
    private Integer likeCount;
    /** 发布时间 */
    private String publishTime;
}
