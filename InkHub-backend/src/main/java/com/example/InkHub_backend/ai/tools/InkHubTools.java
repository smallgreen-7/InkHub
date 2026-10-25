package com.example.InkHub_backend.ai.tools;

import com.example.InkHub_backend.dto.ArticleQueryDTO;
import com.example.InkHub_backend.entity.Article;
import com.example.InkHub_backend.entity.User;
import com.example.InkHub_backend.mapper.ArticleMapper;
import com.example.InkHub_backend.mapper.UserMapper;
import com.example.InkHub_backend.vo.ArticleBriefVO;
import com.example.InkHub_backend.vo.ArticleVO;
import com.example.InkHub_backend.vo.ToolArticleStatsVO;
import com.example.InkHub_backend.vo.UserBriefVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InkHubTools {

    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Tool(description = "搜索站内热门或最新文章，可按关键词过滤，按浏览量(热门)或发布时间(最新)排序，返回文章标题和摘要")
    public List<ArticleBriefVO> searchHotArticles(
            @ToolParam(description = "搜索关键词，可选，为空则返回全部") String keyword,
            @ToolParam(description = "排序方式：hot=按热度(浏览量+点赞)，latest=按发布时间") String sortBy,
            @ToolParam(description = "返回数量，默认5") Integer limit
    ) {
        int size = (limit == null || limit <= 0) ? 5 : Math.min(limit, 20);
        String sort = (sortBy == null || sortBy.isBlank()) ? "hot" : sortBy;
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;

        ArticleQueryDTO query = new ArticleQueryDTO();
        query.setKeyword(kw);
        query.setSort(sort);

        List<ArticleVO> articles = articleMapper.selectArticlePage(query, 0, size);

        List<ArticleBriefVO> result = new ArrayList<>();
        for (var a : articles) {
            result.add(new ArticleBriefVO(
                    a.getId(),
                    a.getTitle(),
                    a.getSummary(),
                    a.getAuthorName(),
                    a.getViewCount(),
                    a.getLikeCount(),
                    a.getPublishTime() != null ? a.getPublishTime().format(DATE_FMT) : ""
            ));
        }
        log.info("Tool searchHotArticles: keyword={}, sort={}, returned={}", kw, sort, result.size());
        return result;
    }

    @Tool(description = "获取指定文章的统计数据：浏览量、点赞数、收藏数、评论数")
    public ToolArticleStatsVO getArticleStats(
            @ToolParam(description = "文章ID") Long articleId
    ) {
        if (articleId == null) return null;
        Article a = articleMapper.selectById(articleId);
        if (a == null) return null;
        log.info("Tool getArticleStats: articleId={}", articleId);
        return new ToolArticleStatsVO(
                a.getId(), a.getTitle(),
                a.getViewCount(), a.getLikeCount(),
                a.getFavoriteCount(), a.getCommentCount()
        );
    }

    @Tool(description = "按用户名或昵称搜索站内作者，返回作者信息和发文数")
    public List<UserBriefVO> searchUsers(
            @ToolParam(description = "搜索关键词") String keyword,
            @ToolParam(description = "返回数量，默认5") Integer limit
    ) {
        int size = (limit == null || limit <= 0) ? 5 : Math.min(limit, 20);
        String kw = (keyword == null || keyword.isBlank()) ? "" : keyword;

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(User::getNickname, kw)
                .or()
                .like(User::getUsername, kw)
                .last("LIMIT " + size);

        List<User> users = userMapper.selectList(wrapper);
        List<UserBriefVO> result = new ArrayList<>();
        for (User u : users) {
            long articleCount = articleMapper.selectCount(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getAuthorId, u.getId())
                            .eq(Article::getStatus, 1));
            result.add(new UserBriefVO(
                    u.getId(), u.getUsername(), u.getNickname(),
                    u.getAvatar(), (int) articleCount
            ));
        }
        log.info("Tool searchUsers: keyword={}, returned={}", kw, result.size());
        return result;
    }
}
