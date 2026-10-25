package com.example.InkHub_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知实体，对应表notification
 */
@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收者 id（被回复/被点赞的人） */
    private Long userId;

    /** 触发者 id（谁回复/谁点赞） */
    private Long fromUserId;

    /** 类型：1 评论回复 / 2 点赞 / 3 收藏 */
    private Integer type;

    /** 通知文案 */
    private String content;

    /** 相关文章 id */
    private Long articleId;

    /** 相关评论 id */
    private Long commentId;

    /** 0 未读 / 1 已读 */
    private Integer isRead;

    /** 评论/点赞/收藏 时间 */
    private LocalDateTime createTime;
}