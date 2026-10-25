package com.example.InkHub_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.InkHub_backend.entity.Favorite;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface FavoriteMapper extends BaseMapper<Favorite> {

    // 我的收藏：收藏表联文章表（只取已发布文章），按收藏时间倒序
    @Select("""
        SELECT a.id, a.title
        FROM favorite f JOIN article a ON a.id = f.article_id AND a.status = 1
        WHERE f.user_id = #{userId}
        ORDER BY f.create_time DESC
    """)
    List<Map<String, Object>> selectMyFavorites(@Param("userId") Long userId);
}