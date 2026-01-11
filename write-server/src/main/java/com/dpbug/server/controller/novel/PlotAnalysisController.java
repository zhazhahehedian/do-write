package com.dpbug.server.controller.novel;

import cn.dev33.satoken.stp.StpUtil;
import com.dpbug.common.domain.Result;
import com.dpbug.server.model.dto.novel.PlotAnalysisRequest;
import com.dpbug.server.model.vo.novel.PlotAnalysisVO;
import com.dpbug.server.service.novel.PlotAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 章节剧情分析
 */
@RestController
@RequestMapping("/api/novel/analysis")
@RequiredArgsConstructor
public class PlotAnalysisController {

    private final PlotAnalysisService plotAnalysisService;

    /**
     * 分析单个章节（非流式，返回结构化 JSON 结果并落库）
     */
    @PostMapping("/analyze-chapter")
    public Result<PlotAnalysisVO> analyzeChapter(@RequestBody @Valid PlotAnalysisRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        PlotAnalysisVO analysis = plotAnalysisService.analyzeChapter(userId, request.getChapterId(), request.getForce());
        return Result.success(analysis);
    }

    /**
     * 获取章节分析报告
     */
    @GetMapping("/{chapterId}")
    public Result<PlotAnalysisVO> getByChapterId(@PathVariable Long chapterId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(plotAnalysisService.getByChapterId(userId, chapterId));
    }
}

