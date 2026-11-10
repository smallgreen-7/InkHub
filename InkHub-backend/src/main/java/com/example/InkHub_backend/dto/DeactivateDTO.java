package com.example.InkHub_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注销账号：软删除（status=1），需密码确认。
 */
@Data
@Schema(description = "注销账号参数")
public class DeactivateDTO {

    @NotBlank(message = "密码不能为空")
    @Schema(description = "当前密码（用于确认身份）")
    private String password;
}
