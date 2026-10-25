package com.example.InkHub_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * 文章实体，对应数据库表 article（8 张表里的核心表）
 */
@Data
@TableName("article")
public class Article {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 摘要（列表页展示） */
    private String summary;

    /** Markdown 原文，LONGTEXT */
    private String contentMd;

    /** 封面图 URL（可空） */
    private String cover;

    /** 所属分类 id */
    private Long categoryId;

    /** 作者 id（user.id） */
    private Long authorId;

    /** 状态：0 草稿 1 已发布 2 已下架 */
    private Integer status;

    /** 是否置顶：0/1 */
    private Integer top;

    /** 浏览量 */
    private Integer viewCount;

    /** 点赞数 */
    private Integer likeCount;

    /** 收藏数 */
    private Integer favoriteCount;

    /** 评论数 */
    private Integer commentCount;

    /** 发布时间（可空，发布时填） */
    private LocalDateTime publishTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
