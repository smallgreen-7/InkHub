package com.example.InkHub_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** AI 摘要请求体 */
@Data
public class SummarizeRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长 100 字")
    private String title;

    @NotBlank(message = "正文不能为空")
    @Size(max = 20000, message = "正文过长")
    private String contentMd;
}
