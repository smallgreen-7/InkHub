package com.example.InkHub_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体，对应表 comment
 */
@Data
@TableName("comment")
public class Comment {

    /** 主键*/
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属文章 id */
    private Long articleId;

    /** 评论人 id */
    private Long userId;

    /** NULL = 一级评论；有值 = 回复某条评论 */
    private Long parentId;

    /** 评论内容 */
    private String content;

    /** 评论时间 */
    private LocalDateTime createTime;
}
