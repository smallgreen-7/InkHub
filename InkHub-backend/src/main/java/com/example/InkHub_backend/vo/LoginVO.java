package com.example.InkHub_backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

// 登录返回：token + 用户基本信息
@Data
@Schema(description = "登录返回结果")
public class LoginVO {
    @Schema(description = "JWT token")
    private String token;
    @Schema(description = "用户 id")
    private Long userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "头像 URL")
    private String avatar;
    @Schema(description = "角色：1 普通用户 / 2 管理员")
    private Integer role;
}