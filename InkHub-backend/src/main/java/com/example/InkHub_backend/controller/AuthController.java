package com.example.InkHub_backend.controller;

import com.example.InkHub_backend.common.R;
import com.example.InkHub_backend.dto.LoginDTO;
import com.example.InkHub_backend.dto.RegisterDTO;
import com.example.InkHub_backend.dto.ResetCodeDTO;
import com.example.InkHub_backend.dto.ResetPasswordDTO;
import com.example.InkHub_backend.service.UserService;
import com.example.InkHub_backend.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证接口", description = "注册、登录、忘记密码，登录成功返回 JWT token")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户注册", description = "注册成功即普通用户（role=1），管理员需后续在数据库改 role=2")
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return R.ok();
    }

    @Operation(summary = "用户登录", description = "返回 token 与用户信息；后续请求在 Authorization 头带 Bearer token")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(userService.login(dto));
    }

    @Operation(summary = "找回密码-发送验证码",
            description = "无需登录；给注册邮箱发 6 位验证码，5 分钟有效，同一邮箱 60 秒内只能发一次。" +
                    "邮箱未注册时也返回成功（防账号枚举）")
    @PostMapping("/password/code")
    public R<Void> sendResetCode(@Valid @RequestBody ResetCodeDTO dto) {
        userService.sendResetCode(dto);
        return R.ok();
    }

    @Operation(summary = "找回密码-重置密码", description = "无需登录；邮箱 + 验证码 + 新密码，验证码一次性使用")
    @PostMapping("/password/reset")
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(dto);
        return R.ok();
    }
}
