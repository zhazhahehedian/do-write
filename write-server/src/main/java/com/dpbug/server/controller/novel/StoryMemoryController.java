package com.dpbug.server.controller.novel;

import cn.dev33.satoken.stp.StpUtil;
import com.dpbug.common.domain.PageResult;
import com.dpbug.common.domain.Result;
import com.dpbug.server.model.dto.novel.ForeshadowResolveRequest;
import com.dpbug.server.model.dto.novel.MemorySearchRequest;
import com.dpbug.server.model.dto.novel.StoryMemoryQueryRequest;
import com.dpbug.server.model.vo.novel.MemoryStatisticsVO;
import com.dpbug.server.model.vo.novel.StoryMemoryVO;
import com.dpbug.server.service.novel.StoryMemoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author dpbug
 * @description 故事记忆控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/novel/memory")
@RequiredArgsConstructor
public class StoryMemoryController {

    private final StoryMemoryService storyMemoryService;

    /**
     * 语义检索相关记忆
     */
    @PostMapping("/search")
    public Result<List<StoryMemoryVO>> search(@RequestBody @Valid MemorySearchRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<StoryMemoryVO> memories = storyMemoryService.searchRelatedMemories(
                userId,
                request.getProjectId(),
                request.getQuery(),
                request.getTopK()
        );
        return Result.success(memories);
    }

    /**
     * 分页列表查询
     */
    @PostMapping("/list")
    public Result<PageResult<StoryMemoryVO>> list(@RequestBody @Valid StoryMemoryQueryRequest request) {
        PageResult<StoryMemoryVO> result = storyMemoryService.listByProject(request);
        return Result.success(result);
    }

    /**
     * 获取章节记忆
     */
    @GetMapping("/chapter/{chapterId}")
    public Result<List<StoryMemoryVO>> listByChapter(@PathVariable Long chapterId) {
        List<StoryMemoryVO> memories = storyMemoryService.listByChapter(chapterId);
        return Result.success(memories);
    }

    /**
     * 获取伏笔追踪列表
     */
    @GetMapping("/foreshadows")
    public Result<List<StoryMemoryVO>> getForeshadows(@RequestParam Long projectId) {
        List<StoryMemoryVO> foreshadows = storyMemoryService.getPendingForeshadows(projectId);
        return Result.success(foreshadows);
    }

    /**
     * 标记伏笔已回收
     */
    @PostMapping("/resolve")
    public Result<Void> resolveForeshadow(@RequestBody @Valid ForeshadowResolveRequest request) {
        storyMemoryService.resolveForeshadow(request.getMemoryId(), request.getResolvedAtChapterId());
        return Result.success();
    }

    /**
     * 获取记忆统计信息
     */
    @GetMapping("/statistics")
    public Result<MemoryStatisticsVO> getStatistics(@RequestParam Long projectId) {
        MemoryStatisticsVO statistics = storyMemoryService.getStatistics(projectId);
        return Result.success(statistics);
    }

    /**
     * 重新提取章节记忆
     */
    @GetMapping("/re-extract/{chapterId}")
    public Result<Void> reExtractMemories(@PathVariable Long chapterId) {
        Long userId = StpUtil.getLoginIdAsLong();
        storyMemoryService.reExtractMemories(userId, chapterId);
        return Result.success();
    }
}
