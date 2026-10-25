package com.example.InkHub_backend.vo;

import lombok.Data;

/** 回答引用来源，前端渲染需要的溯源信息 */
@Data
public class ChatSourceVO {
    private Long articleId;
    private String title;
    private String summary;
    private String authorname;
    private double score;

}
