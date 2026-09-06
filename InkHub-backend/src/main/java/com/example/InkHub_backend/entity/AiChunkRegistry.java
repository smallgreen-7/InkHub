package com.example.InkHub_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 分块登记表：记录每篇文章在 Qdrant 里的向量点 id（art{articleId}-{i}）
 * Qdrant 只存向量，这张表管"文章 ↔ 向量点"的映射，删除文章时按它精确清点
 */
@Data
@TableName("ai_chunk_registry")
public class AiChunkRegistry {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 文章 id */
    private Long articleId;
    /** Qdrant 点 id（= art{articleId}-{chunkIndex}） */
    private String pointId;
    /** 创建时间 */
    private LocalDateTime createTime;
}
