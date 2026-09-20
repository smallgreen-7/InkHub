package com.example.InkHub_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.InkHub_backend.entity.Article;
import com.example.InkHub_backend.vo.ArticleDetailVO;
import com.example.InkHub_backend.vo.ArticleVO;
import com.example.InkHub_backend.vo.CategoryVO;
import com.example.InkHub_backend.vo.TagVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ArticleMapper extends BaseMapper<Article> {

    // 文章流：最新/最热 + 可选分类/关键词过滤（只查已发布）
    // SQL 全部在 resources/mapper/ArticleMapper.xml（接口零注解）
    List<ArticleVO> selectArticlePage(@Param("q") Object q,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    // 文章流总数（和上面同一个 where，分页要用）
    long countArticlePage(@Param("q") Object q);

    // 按标签查文章（先查中间表拿 article_id，再查文章；排序规则同文章流）
    List<ArticleVO> selectPageByTag(@Param("tagId") Long tagId,
                                    @Param("offset") int offset,
                                    @Param("size") int size);

    // 按标签查总数
    long countByTag(@Param("tagId") Long tagId);

    // 文章详情（含分类名/作者昵称）
    ArticleDetailVO selectDetail(@Param("id") Long id);

    // 分类列表 + 文章数（分类模块用）
    List<CategoryVO> selectCategoryWithCount();

    // 标签 + 文章数（热门标签）
    List<TagVO> selectTagWithCount();

    // 同分类的已发布文章（排除自己，按浏览量倒序）——相关文章推荐
    List<ArticleVO> selectRelatedByCategory(@Param("categoryId") Long categoryId,
                                            @Param("id") Long id,
                                            @Param("limit") int limit);

    // 同标签的已发布文章（排除自己，按浏览量倒序）——相关文章推荐
    List<ArticleVO> selectRelatedByTag(@Param("tagId") Long tagId,
                                       @Param("id") Long id,
                                       @Param("limit") int limit);

    // 按 id 批量取文章卡片（AI 语义搜索召回后回表用，一次查完避免 N+1）
    List<ArticleVO> selectVOByIds(@Param("ids") List<Long> ids);

    // BM25 全文检索：MySQL FULLTEXT + ngram 分词，返回匹配文章
    List<Article> selectByFulltext(@Param("query") String query,
                                  @Param("limit") int limit);

}