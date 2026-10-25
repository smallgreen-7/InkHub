package com.example.InkHub_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.InkHub_backend.common.BusinessException;
import com.example.InkHub_backend.common.R;
import com.example.InkHub_backend.service.NotificationService;
import com.example.InkHub_backend.vo.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "通知接口", description = "站内通知的列表、未读数、已读（需要登录）")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    //通用取当前登录的用户ID
    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        throw new BusinessException(401, "未登录");
    }

    @Operation(summary = "我的通知（分页）")
    @GetMapping
    public R<Page<NotificationVO>> page(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
                                        @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(notificationService.page(currentUserId(), pageNum, pageSize));
    }

    @Operation(summary = "未读通知数", description = "导航栏红点轮询用")
    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(notificationService.unreadCount(currentUserId()));
    }

    @Operation(summary = "单条通知已读")
    @PutMapping("/{id}/read")
    public R<Void> read(@Parameter(description = "通知 id") @PathVariable Long id) {
        notificationService.read(currentUserId(), id);
        return R.ok();
    }

    @Operation(summary = "全部已读")
    @PutMapping("/read-all")
    public R<Void> readAll() {
        notificationService.readAll(currentUserId());
        return R.ok();
    }
}