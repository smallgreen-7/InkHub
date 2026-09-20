package com.example.InkHub_backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 工具返回：用户简要信息（Function Calling 用） */
@Data
@AllArgsConstructor
public class UserBriefVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer articleCount;
}
