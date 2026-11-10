package com.example.InkHub_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 通知偏好：1 开启 / 0 关闭，对应 notification.type 的 1 评论 / 2 点赞 / 3 收藏。
 */
@Data
@Schema(description = "通知偏好参数")
public class NotifyPrefDTO {

    @Min(0)
    @Max(1)
    @Schema(description = "评论/回复通知：1 开 0 关")
    private Integer notifyComment;

    @Min(0)
    @Max(1)
    @Schema(description = "点赞通知：1 开 0 关")
    private Integer notifyLike;

    @Min(0)
    @Max(1)
    @Schema(description = "收藏通知：1 开 0 关")
    private Integer notifyFavorite;
}
