package com.example.InkHub_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改用户名：唯一键字段，改之前必须验证密码。
 * 说明：JWT 里存的是 userId，不存用户名，所以改名不需要重新签发 token。
 */
@Data
@Schema(description = "修改用户名参数")
public class ChangeUsernameDTO {

    @NotBlank(message = "新用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字、下划线")
    @Schema(description = "新用户名（3-20 位，字母数字下划线）")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "当前密码（用于确认身份）")
    private String password;
}
