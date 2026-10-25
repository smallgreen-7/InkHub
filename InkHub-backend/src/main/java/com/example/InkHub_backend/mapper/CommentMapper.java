package com.example.InkHub_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.InkHub_backend.entity.Comment;
import com.example.InkHub_backend.vo.CommentVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface CommentMapper extends BaseMapper<Comment> {

    // 文章评论列表：联表拿昵称头像，一级评论在前，回复跟着所属一级评论排
    @Select("""
        SELECT c.id, c.article_id, c.user_id, u.nickname, u.avatar,c.parent_id, c.content, c.create_time
        FROM comment c LEFT JOIN user u ON u.id = c.user_id
        WHERE c.article_id = #{articleId}
        ORDER BY IF(c.parent_id IS NULL, c.id, c.parent_id) DESC, c.id ASC 
    """)
    List<CommentVO> selectByArticle(@Param("articleId") Long articleId);

    // 管理端：全部评论（联表文章标题 + 评论人昵称），按时间倒序
    @Select("""
        SELECT c.id, c.article_id, a.title AS article_title,c.user_id, u.nickname, c.content, c.create_time
        FROM comment c LEFT JOIN article a ON a.id = c.article_id LEFT JOIN user u ON u.id = c.user_id
        ORDER BY c.create_time DESC
    """)
    List<Map<String, Object>> selectAllForAdmin();
}
