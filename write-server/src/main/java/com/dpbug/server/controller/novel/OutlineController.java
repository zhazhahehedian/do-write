package com.dpbug.server.controller.novel;

import cn.dev33.satoken.stp.StpUtil;
import com.dpbug.common.domain.Result;
import com.dpbug.server.model.dto.novel.OutlineCreateRequest;
import com.dpbug.server.model.dto.novel.ExpandedChaptersGenerateRequest;
import com.dpbug.server.model.dto.novel.OutlineExpandApplyRequest;
import com.dpbug.server.model.dto.novel.OutlineExpandRequest;
import com.dpbug.server.model.dto.novel.OutlineUpdateRequest;
import com.dpbug.server.model.entity.novel.NovelOutline;
import com.dpbug.server.model.vo.novel.ChapterVO;
import com.dpbug.server.model.vo.novel.GenerationTaskVO;
import com.dpbug.server.model.vo.novel.OutlineVO;
import com.dpbug.server.service.novel.OutlineService;
import com.dpbug.server.service.novel.PlotExpansionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 大纲控制器
 *
 * @author dpbug
 */
@Slf4j
@RestController
@RequestMapping("/api/novel/outline")
@RequiredArgsConstructor
public class OutlineController {

    private final OutlineService outlineService;
    private final PlotExpansionService plotExpansionService;

    /**
     * 获取项目所有大纲列表
     */
    @GetMapping("/list")
    public Result<List<OutlineVO>> list(
            @RequestParam @NotNull(message = "项目ID不能为空") Long projectId) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<OutlineVO> outlines = outlineService.listByProject(userId, projectId);
        return Result.success(outlines);
    }

    /**
     * 创建大纲
     */
    @PostMapping("/create")
    public Result<OutlineVO> create(@RequestBody @Valid OutlineCreateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        NovelOutline outline = new NovelOutline();
        outline.setOrderIndex(request.getOrderIndex());
        outline.setTitle(request.getTitle());
        outline.setContent(request.getContent());
        outline.setStructure(request.getStructure());

        outlineService.createBatch(userId, request.getProjectId(), List.of(outline));
        OutlineVO created = outlineService.getById(userId, outline.getId());
        return Result.success(created);
    }

    /**
     * 获取大纲详情
     */
    @GetMapping("/{id}")
    public Result<OutlineVO> getById(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        OutlineVO outline = outlineService.getById(userId, id);
        return Result.success(outline);
    }

    /**
     * 更新大纲
     */
    @PostMapping("/{id}/update")
    public Result<OutlineVO> update(
            @PathVariable Long id,
            @RequestBody @Valid OutlineUpdateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        NovelOutline outline = new NovelOutline();
        outline.setId(id);
        outline.setOrderIndex(request.getOrderIndex());
        outline.setTitle(request.getTitle());
        outline.setContent(request.getContent());
        outline.setStructure(request.getStructure());

        outlineService.update(userId, outline);
        OutlineVO updated = outlineService.getById(userId, id);
        return Result.success(updated);
    }

    /**
     * 删除大纲
     */
    @PostMapping("/{id}/delete")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        outlineService.delete(userId, id);
        return Result.success();
    }

    /**
     * 获取项目大纲数量
     */
    @GetMapping("/count")
    public Result<Integer> count(
            @RequestParam @NotNull(message = "项目ID不能为空") Long projectId) {
        int count = outlineService.countByProject(projectId);
        return Result.success(count);
    }

    // ==================== 大纲展开（one-to-many）====================

    /**
     * 展开规划预览（SSE流式）
     * <p>
     * 根据大纲内容生成N个子章节规划，不创建章节记录
     */
    @PostMapping(value = "/{outlineId}/expand/preview", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> expandPreview(
            @PathVariable Long outlineId,
            @RequestBody @Valid OutlineExpandRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        request.setOutlineId(outlineId);
        log.info("大纲展开预览: userId={}, outlineId={}, targetCount={}",
                userId, outlineId, request.getTargetChapterCount());
        return plotExpansionService.previewExpansion(userId, request)
                .map(data -> "data: " + data + "\n\n");
    }

    /**
     * 应用展开（创建章节记录）
     * <p>
     * 将预览生成的规划写入数据库，创建章节记录
     */
    @PostMapping("/{outlineId}/expand/apply")
    public Result<List<ChapterVO>> expandApply(
            @PathVariable Long outlineId,
            @RequestBody @Valid OutlineExpandApplyRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        request.setOutlineId(outlineId);
        log.info("大纲展开应用: userId={}, outlineId={}, chapterCount={}, force={}",
                userId, outlineId, request.getChapterPlans().size(), request.getForce());
        List<ChapterVO> chapters = plotExpansionService.applyExpansion(userId, request);
        return Result.success(chapters);
    }

    /**
     * 一纲多章：批量生成已展开的子章节（后台任务）
     */
    @PostMapping("/{outlineId}/expand/generate/async")
    public Result<GenerationTaskVO> generateExpandedChaptersAsync(
            @PathVariable Long outlineId,
            @RequestBody @Valid ExpandedChaptersGenerateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        GenerationTaskVO task = plotExpansionService.generateExpandedChaptersAsync(userId, outlineId, request);
        return Result.success(task);
    }

    /**
     * 获取大纲已展开的子章节列表
     */
    @GetMapping("/{outlineId}/chapters")
    public Result<List<ChapterVO>> getExpandedChapters(@PathVariable Long outlineId) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<ChapterVO> chapters = plotExpansionService.getExpandedChapters(userId, outlineId);
        return Result.success(chapters);
    }

    /**
     * 检查大纲是否已展开
     */
    @GetMapping("/{outlineId}/expanded")
    public Result<Boolean> isExpanded(@PathVariable Long outlineId) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 权限校验：避免通过 outlineId 探测他人项目数据
        outlineService.getById(userId, outlineId);
        boolean expanded = plotExpansionService.isExpanded(outlineId);
        return Result.success(expanded);
    }

    /**
     * 删除大纲的展开章节（重新展开前的清理）
     */
    @PostMapping("/{outlineId}/expand/reset")
    public Result<Void> resetExpansion(@PathVariable Long outlineId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("重置大纲展开: userId={}, outlineId={}", userId, outlineId);
        plotExpansionService.deleteExpandedChapters(userId, outlineId);
        return Result.success();
    }
}
