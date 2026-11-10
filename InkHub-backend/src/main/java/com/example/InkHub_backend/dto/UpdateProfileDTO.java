package com.example.InkHub_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改基本资料（昵称 / 头像 / 邮箱）
 * 只传需要修改的字段，null 表示不改。
 */
@Data
@Schema(description = "修改基本资料参数")
public class UpdateProfileDTO {

    @Size(max = 20, message = "昵称最长 20 字")
    @Schema(description = "昵称")
    private String nickname;

    @Size(max = 255, message = "头像地址过长")
    @Schema(description = "头像 URL")
    private String avatar;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最长 100 字")
    @Schema(description = "邮箱（可用于找回密码）")
    private String email;
}
