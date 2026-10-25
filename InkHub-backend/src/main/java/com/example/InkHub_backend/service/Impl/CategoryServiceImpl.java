package com.example.InkHub_backend.service.Impl;

import com.example.InkHub_backend.mapper.ArticleMapper;
import com.example.InkHub_backend.mapper.CategoryMapper;
import com.example.InkHub_backend.service.CategoryService;
import com.example.InkHub_backend.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;   // 用 ArticleMapper 手写 SQL 查文章数

    @Override
    public List<CategoryVO> listWithCount() {
        return articleMapper.selectCategoryWithCount();
    }
}
