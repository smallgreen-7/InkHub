package com.example.InkHub_backend.controller;

import com.example.InkHub_backend.common.BusinessException;
import com.example.InkHub_backend.common.R;
import com.example.InkHub_backend.dto.ChangePasswordDTO;
import com.example.InkHub_backend.dto.ChangeUsernameDTO;
import com.example.InkHub_backend.dto.DeactivateDTO;
import com.example.InkHub_backend.dto.NotifyPrefDTO;
import com.example.InkHub_backend.dto.UpdateProfileDTO;
import com.example.InkHub_backend.mapper.FavoriteMapper;
import com.example.InkHub_backend.service.UserService;
import com.example.InkHub_backend.vo.LoginVO;
import com.example.InkHub_backend.vo.NotifyPrefVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户接口", description = "当前登录用户的个人信息、资料修改、密码与安全、通知偏好、收藏列表")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FavoriteMapper favoriteMapper;

    // 取当前登录用户 id（所有 Controller 都能用的套路）
    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        throw new BusinessException(401, "未登录");
    }

    // ---------- 基本资料 ----------

    @Operation(summary = "我的信息", description = "需登录；返回昵称、头像、邮箱、角色、通知偏好")
    @GetMapping("/me")
    public R<LoginVO> me() {
        return R.ok(userService.me(currentUserId()));
    }

    @Operation(summary = "修改资料", description = "需登录；body 传 nickname/avatar/email，只传需要改的字段（email 传空串表示解绑）")
    @PutMapping("/profile")
    public R<Void> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        userService.updateProfile(currentUserId(), dto);
        return R.ok();
    }

    @Operation(summary = "修改用户名", description = "需登录；需带当前密码确认；JWT 里存的是 userId，改名后 token 依然有效")
    @PutMapping("/username")
    public R<Void> changeUsername(@Valid @RequestBody ChangeUsernameDTO dto) {
        userService.changeUsername(currentUserId(), dto);
        return R.ok();
    }

    // ---------- 密码与安全 ----------

    @Operation(summary = "修改密码", description = "需登录；校验原密码；改完前端应强制重新登录")
    @PutMapping("/password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(currentUserId(), dto);
        return R.ok();
    }

    @Operation(summary = "注销账号", description = "需登录；软删除（status=1），需密码确认；最后一个管理员不允许注销")
    @DeleteMapping("/account")
    public R<Void> deactivate(@Valid @RequestBody DeactivateDTO dto) {
        userService.deactivate(currentUserId(), dto);
        return R.ok();
    }

    // ---------- 通知偏好 ----------

    @Operation(summary = "查询通知偏好", description = "需登录；返回评论/点赞/收藏三个开关（1 开 0 关）")
    @GetMapping("/notify-pref")
    public R<NotifyPrefVO> notifyPref() {
        return R.ok(userService.notifyPref(currentUserId()));
    }

    @Operation(summary = "修改通知偏好", description = "需登录；只传需要改的开关")
    @PutMapping("/notify-pref")
    public R<Void> updateNotifyPref(@Valid @RequestBody NotifyPrefDTO dto) {
        userService.updateNotifyPref(currentUserId(), dto);
        return R.ok();
    }

    // ---------- 收藏 ----------

    @Operation(summary = "我的收藏列表", description = "需登录；返回当前用户收藏的文章（标题+收藏时间）")
    @GetMapping("/favorites")
    public R<List<Map<String, Object>>> favorites() {
        return R.ok(favoriteMapper.selectMyFavorites(currentUserId()));
    }
}
