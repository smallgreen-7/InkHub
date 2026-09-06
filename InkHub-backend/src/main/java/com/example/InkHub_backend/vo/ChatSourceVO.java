package com.example.InkHub_backend.vo;

import lombok.Data;

/** 回答引用来源（前端渲染可点击卡片） */
@Data
public class ChatSourceVO {
    private Long articleId;
    private String title;
    private double score;
}
