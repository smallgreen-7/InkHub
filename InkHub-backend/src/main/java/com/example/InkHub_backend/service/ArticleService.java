package com.example.InkHub_backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.InkHub_backend.dto.ArticleDTO;
import com.example.InkHub_backend.dto.ArticleQueryDTO;
import com.example.InkHub_backend.vo.ArticleDetailVO;
import com.example.InkHub_backend.vo.ArticleVO;

import java.util.List;

public interface ArticleService {

    Page<ArticleVO> page(ArticleQueryDTO q);          // 文章流

    ArticleDetailVO detail(Long id);                  // 详情（浏览量+1）

    Long create(Long userId, ArticleDTO dto);         // 发布/存草稿

    void update(Long userId, Long articleId, ArticleDTO dto);  // 编辑（作者本人）

    void delete(Long userId, Long articleId);         // 删除（作者本人）

    Page<ArticleVO> myArticles(Long userId, int pageNum, int pageSize);  // 我的文章（含草稿）

    List<ArticleVO> related(Long id);

    List<ArticleVO> semanticSearch(String keyword);   // AI 语义搜索（向量召回）
}
