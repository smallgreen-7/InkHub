package com.example.InkHub_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.InkHub_backend.entity.ArticleTag;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ArticleTagMapper extends BaseMapper<ArticleTag> {

    // 查一篇文章的所有标签名
    @Select("""
        SELECT t.name FROM tag t
        JOIN article_tag at ON at.tag_id = t.id
        WHERE at.article_id = #{articleId}
        ORDER BY t.id
    """)
    List<String> selectTagNamesByArticle(@Param("articleId") Long articleId);

    // 查一篇文章的所有标签 id
    @Select("SELECT tag_id FROM article_tag WHERE article_id = #{articleId} ORDER BY id")
    List<Long> selectTagIdsByArticle(@Param("articleId") Long articleId);
}