package com.example.InkHub_backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 工具返回：文章简要信息（Function Calling 用） */
@Data
@AllArgsConstructor
public class ArticleBriefVO {
    private Long id;
    private String title;
    private String summary;
    private String authorName;
    private Integer viewCount;
    private Integer likeCount;
    private String publishTime;
}
