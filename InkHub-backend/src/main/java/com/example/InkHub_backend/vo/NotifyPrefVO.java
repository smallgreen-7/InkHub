package com.example.InkHub_backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通知偏好返回体
 */
@Data
@Schema(description = "通知偏好")
public class NotifyPrefVO {

    @Schema(description = "评论/回复通知：1 开 0 关")
    private Integer notifyComment;

    @Schema(description = "点赞通知：1 开 0 关")
    private Integer notifyLike;

    @Schema(description = "收藏通知：1 开 0 关")
    private Integer notifyFavorite;
}
