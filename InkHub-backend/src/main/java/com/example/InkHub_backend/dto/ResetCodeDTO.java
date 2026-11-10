package com.example.InkHub_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 找回密码第一步：用注册邮箱申请验证码。
 */
@Data
@Schema(description = "发送找回密码验证码参数")
public class ResetCodeDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "注册邮箱")
    private String email;
}
