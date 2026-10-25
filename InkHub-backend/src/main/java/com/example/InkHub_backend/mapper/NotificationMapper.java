package com.example.InkHub_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.InkHub_backend.entity.Notification;
import com.example.InkHub_backend.vo.NotificationVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface NotificationMapper extends BaseMapper<Notification> {

    // 我的通知（联表：触发者昵称/头像 + 文章标题），按时间倒序
    @Select("""
            SELECT n.id, n.type, n.content, n.article_id, n.comment_id, n.is_read, n.create_time,
                   u.nickname AS from_nickname, u.avatar AS from_avatar,a.title AS article_title
            FROM notification n LEFT JOIN user u ON u.id = n.from_user_id LEFT JOIN article a ON a.id = n.article_id
            WHERE n.user_id = #{userId}
            ORDER BY n.create_time DESC
            LIMIT #{offset}, #{size}
            """)
    List<NotificationVO> selectByUser(@Param("userId") Long userId,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    // 未读数
    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    long countUnread(@Param("userId") Long userId);
}
