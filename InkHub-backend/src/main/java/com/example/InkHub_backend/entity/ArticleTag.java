package com.example.InkHub_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 文章-标签中间表实体，对应表 article_tag
 * 一篇文章可以打多个标签，一个标签下可以有多篇文章。
*/

@Data
@TableName("article_tag")
public class ArticleTag {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文章 id */
    private Long articleId;

    /** 标签 id */
    private Long tagId;
}
