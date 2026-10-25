package com.example.InkHub_backend.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.InkHub_backend.common.BusinessException;
import com.example.InkHub_backend.entity.Notification;
import com.example.InkHub_backend.mapper.NotificationMapper;
import com.example.InkHub_backend.service.NotificationService;
import com.example.InkHub_backend.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override           //我的通知
    public Page<NotificationVO> page(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;      //pageNum页码，pageSize每页条数
        List<NotificationVO> records = notificationMapper.selectByUser(userId, offset, pageSize);
        long total = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>().eq(Notification::getUserId, userId));
        Page<NotificationVO> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(records);
        return page;
    }

    @Override           //未读的通知，只返回具体数字，前端显示为红点+数字
    public long unreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override           //单条已读的通知
    public void read(Long userId, Long id) {
        // 只能已读自己的通知（防越权）
        int updated = notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1));
        if (updated == 0) {
            throw new BusinessException("通知不存在");
        }
    }

    @Override           //全部已读的通知
    public void readAll(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }
}