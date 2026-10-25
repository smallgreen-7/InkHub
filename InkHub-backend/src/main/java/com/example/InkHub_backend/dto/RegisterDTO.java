package com.example.InkHub_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "注册参数")
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20 位")
    @Schema(description = "用户名（3-20 位）", minLength = 3, maxLength = 20)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6-20 位")
    @Schema(description = "密码（6-20 位）",  minLength = 6, maxLength = 20)
    private String password;

    @Schema(description = "昵称（选填，默认同用户名）")
    private String nickname;
}
