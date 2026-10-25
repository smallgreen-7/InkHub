package com.example.InkHub_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * 点赞实体，对应表 article_like
 * 表上有唯一索引 UNIQUE(article_id, user_id)：
 */
@Data
@TableName("article_like")
public class ArticleLike {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被点赞的文章 id */
    private Long articleId;

    /** 点赞用户 id */
    private Long userId;

    /** 点赞时间 */
    private LocalDateTime createTime;
}
