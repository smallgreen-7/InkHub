package com.example.InkHub_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * 标签实体，对应表tag
 */
@Data
@TableName("tag")
public class Tag {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签名 */
    private String name;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
