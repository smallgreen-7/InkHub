package com.example.InkHub_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码：必须校验原密码，防止 token 被盗后直接改密码。
 */
@Data
@Schema(description = "修改密码参数")
public class ChangePasswordDTO {

    @NotBlank(message = "原密码不能为空")
    @Schema(description = "原密码")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度 6-20 位")
    @Schema(description = "新密码（6-20 位）")
    private String newPassword;
}
