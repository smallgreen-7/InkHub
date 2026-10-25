package com.example.InkHub_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * 收藏实体，对于表favorite
 * 表上有唯一索引 UNIQUE(article_id, user_id)，
 * 同一用户对同一文章只能收藏一次，重复收藏靠唯一索引兜底（幂等）。
 */
@Data
@TableName("favorite")
public class Favorite {

    /** 主键*/
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被收藏的文章 id */
    private Long articleId;

    /** 收藏用户 id */
    private Long userId;

    /** 收藏时间 */
    private LocalDateTime createTime;
}
