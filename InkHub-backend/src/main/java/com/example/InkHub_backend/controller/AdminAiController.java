package com.example.InkHub_backend.controller;

import com.example.InkHub_backend.common.R;
import com.example.InkHub_backend.service.AiIndexService;
import com.example.InkHub_backend.vo.AiStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 向量索引管理；就是向量数据库的索引，给向量检索用的。（ADMIN 权限才能用；/api/admin/**  ） */
@Tag(name = "AI 索引管理")
@RestController     //类里面所有方法返回值，直接作为 JSON形式丢给前端，不用单独写 response 渲染页面
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {

    private final AiIndexService aiIndexService;

    @Operation(summary = "AI 索引统计")
    @GetMapping("/stats")
    public R<AiStatsVO> stats() {
        return R.ok(aiIndexService.stats());
    }

    @Operation(summary = "向量库异常、索引损坏时全量重建 AI 索引（同步执行，文章多时等待）")
    @PostMapping("/rebuild")
    public R<AiStatsVO> rebuild() {
        return R.ok(aiIndexService.rebuildAll());
    }
}
