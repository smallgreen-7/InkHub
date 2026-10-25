package com.example.InkHub_backend.controller;

import com.example.InkHub_backend.common.BusinessException;
import com.example.InkHub_backend.common.R;
import com.example.InkHub_backend.entity.User;
import com.example.InkHub_backend.mapper.FavoriteMapper;
import com.example.InkHub_backend.mapper.UserMapper;
import com.example.InkHub_backend.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户接口", description = "当前登录用户的个人信息、资料修改、收藏列表")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final FavoriteMapper favoriteMapper;

    // 取当前登录用户 id（所有 Controller 都能用的套路）
    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        throw new BusinessException(401, "未登录");
    }

    // 看个人信息
    @Operation(summary = "我的信息", description = "需登录；返回当前用户基本信息（含昵称、头像、角色）")
    @GetMapping("/me")
    public R<LoginVO> me() {
        User user = userMapper.selectById(currentUserId());
        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        return R.ok(vo);
    }

    // 改昵称/头像
    @Operation(summary = "修改资料", description = "需登录；body 传需要修改的字段（nickname/avatar），只传非空字段生效")
    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestBody User update) {
        User user = userMapper.selectById(currentUserId());
        if (update.getNickname() != null) user.setNickname(update.getNickname());
        if (update.getAvatar() != null) user.setAvatar(update.getAvatar());
        userMapper.updateById(user);
        return R.ok();
    }

    // 我的收藏（收藏表联文章表拿标题，按收藏时间倒序）
    @Operation(summary = "我的收藏列表", description = "需登录；返回当前用户收藏的文章（标题+收藏时间）")
    @GetMapping("/favorites")
    public R<List<Map<String, Object>>> favorites() {
        return R.ok(favoriteMapper.selectMyFavorites(currentUserId()));
    }
}
