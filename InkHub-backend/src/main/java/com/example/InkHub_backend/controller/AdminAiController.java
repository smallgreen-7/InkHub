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

/** AI 索引管理（/api/admin/** 已有 ADMIN 权限） */
@Tag(name = "AI 索引管理")
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {

    private final AiIndexService aiIndexService;

    @Operation(summary = "AI 索引统计")
    @GetMapping("/stats")
    public R<AiStatsVO> stats() {
        return R.ok(aiIndexService.stats());
    }

    @Operation(summary = "全量重建 AI 索引（同步执行，文章多时等待）")
    @PostMapping("/rebuild")
    public R<AiStatsVO> rebuild() {
        return R.ok(aiIndexService.rebuildAll());
    }
}
