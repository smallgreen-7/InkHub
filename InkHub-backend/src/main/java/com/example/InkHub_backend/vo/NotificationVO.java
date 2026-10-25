package com.example.InkHub_backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

// 通知页：通知 + 触发者昵称/头像 + 文章标题
@Data
public class NotificationVO {
    private Long id;                // 通知记录id（notification表主键）
    private Integer type;           // 通知类型：1点赞 / 2评论 / 3收藏
    private String content;         // 通知文本内容，例如："评论了你的文章"
    private Long articleId;         // 关联文章id
    private Long commentId;         // 关联评论id；如果是评论类通知，点进去定位到这条评论
    private Integer isRead;         // 是否已读：0未读，1已读。
    private LocalDateTime createTime;// 通知创建时间
    private String fromNickname;    // 触发者昵称（谁点的赞/评的论）【多表组装，通知表没有这个字段】
    private String fromAvatar;      // 触发者头像url【多表组装，通知表没有这个字段】
    private String articleTitle;    // 关联文章标题【多表组装，通知表没有这个字段】
}
