package com.dpbug.server.controller.novel;

import cn.dev33.satoken.stp.StpUtil;
import com.dpbug.common.domain.PageResult;
import com.dpbug.common.domain.Result;
import com.dpbug.server.model.dto.novel.BatchGenerateRequest;
import com.dpbug.server.model.dto.novel.ChapterGenerateRequest;
import com.dpbug.server.model.dto.novel.ChapterPolishRequest;
import com.dpbug.server.model.dto.novel.ChapterQueryRequest;
import com.dpbug.server.model.dto.novel.ChapterUpdateRequest;
import com.dpbug.server.model.vo.novel.ChapterContextVO;
import com.dpbug.server.model.vo.novel.ChapterDetailVO;
import com.dpbug.server.model.vo.novel.ChapterVO;
import com.dpbug.server.model.vo.novel.GenerationTaskVO;
import com.dpbug.server.service.novel.ChapterService;
import com.dpbug.server.service.novel.GenerationTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 章节控制器
 *
 * @author dpbug
 */
@Slf4j
@RestController
@RequestMapping("/api/novel/chapter")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;
    private final GenerationTaskService taskService;

    /**
     * 生成单个章节（SSE流式）
     * 注意：不使用@Valid，改为手动校验，以便在SSE流中返回错误
     */
    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generateChapter(
            @RequestBody ChapterGenerateRequest request) {
        // 手动参数校验，确保错误能以SSE格式返回
        if (request.getProjectId() == null) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"type\":\"error\",\"error\":\"项目ID不能为空\"}")
                    .build());
        }
        if (request.getOutlineId() == null) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"type\":\"error\",\"error\":\"大纲ID不能为空\"}")
                    .build());
        }

        Long userId = StpUtil.getLoginIdAsLong();

        return chapterService.generateChapter(userId, request)
                .map(content -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data("{\"type\":\"chunk\",\"content\":\"" + escapeJson(content) + "\"}")
                        .build())
                .concatWith(Mono.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("{\"type\":\"done\"}")
                        .build()))
                .onErrorResume(e -> {
                    log.error("章节生成失败", e);
                    String errorJson = "{\"type\":\"error\",\"error\":\"" +
                            e.getMessage().replace("\"", "\\\"") + "\"}";
                    return Mono.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(errorJson)
                            .build());
                });
    }

    /**
     * 批量生成章节（返回任务ID）
     */
    @PostMapping("/batch-generate")
    public Result<GenerationTaskVO> batchGenerate(
            @RequestBody @Valid BatchGenerateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long taskId = chapterService.batchGenerate(userId, request);
        GenerationTaskVO task = taskService.getTask(userId, taskId);
        return Result.success(task);
    }

    /**
     * 重新生成章节（SSE流式）
     */
    @PostMapping(value = "/regenerate/{chapterId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> regenerateChapter(
            @PathVariable Long chapterId,
            @RequestBody ChapterGenerateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        return chapterService.regenerateChapter(userId, chapterId, request)
                .map(content -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data("{\"type\":\"chunk\",\"content\":\"" + escapeJson(content) + "\"}")
                        .build())
                .concatWith(Mono.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("{\"type\":\"done\"}")
                        .build()))
                .onErrorResume(e -> {
                    log.error("重新生成章节失败: chapterId={}", chapterId, e);
                    String errorJson = "{\"type\":\"error\",\"error\":\"" +
                            e.getMessage().replace("\"", "\\\"") + "\"}";
                    return Mono.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(errorJson)
                            .build());
                });
    }

    /**
     * 润色章节（SSE流式）
     */
    @PostMapping(value = "/polish", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> polishChapter(
            @RequestBody ChapterPolishRequest request) {
        // 手动参数校验
        if (request.getChapterId() == null) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"type\":\"error\",\"error\":\"章节ID不能为空\"}")
                    .build());
        }

        Long userId = StpUtil.getLoginIdAsLong();

        return chapterService.polishChapter(userId, request)
                .map(content -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data("{\"type\":\"chunk\",\"content\":\"" + escapeJson(content) + "\"}")
                        .build())
                .concatWith(Mono.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("{\"type\":\"done\"}")
                        .build()))
                .onErrorResume(e -> {
                    log.error("润色章节失败: chapterId={}", request.getChapterId(), e);
                    String errorJson = "{\"type\":\"error\",\"error\":\"" +
                            e.getMessage().replace("\"", "\\\"") + "\"}";
                    return Mono.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(errorJson)
                            .build());
                });
    }

    /**
     * AI去味（SSE流式）
     * 将AI生成的文本改写得更像人类作家的手笔
     */
    @PostMapping(value = "/denoise/{chapterId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> denoiseChapter(@PathVariable Long chapterId) {
        Long userId = StpUtil.getLoginIdAsLong();

        return chapterService.denoiseChapter(userId, chapterId)
                .map(content -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data("{\"type\":\"chunk\",\"content\":\"" + escapeJson(content) + "\"}")
                        .build())
                .concatWith(Mono.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("{\"type\":\"done\"}")
                        .build()))
                .onErrorResume(e -> {
                    log.error("AI去味失败: chapterId={}", chapterId, e);
                    String errorJson = "{\"type\":\"error\",\"error\":\"" +
                            e.getMessage().replace("\"", "\\\"") + "\"}";
                    return Mono.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(errorJson)
                            .build());
                });
    }

    /**
     * 获取章节详情
     */
    @GetMapping("/{chapterId}")
    public Result<ChapterDetailVO> getDetail(@PathVariable Long chapterId) {
        Long userId = StpUtil.getLoginIdAsLong();
        ChapterDetailVO detail = chapterService.getDetail(userId, chapterId);
        return Result.success(detail);
    }

    /**
     * 获取章节列表
     */
    @PostMapping("/list")
    public Result<PageResult<ChapterVO>> list(
            @RequestBody @Valid ChapterQueryRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        PageResult<ChapterVO> result = chapterService.list(userId, request);
        return Result.success(result);
    }

    /**
     * 更新章节
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody @Valid ChapterUpdateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        chapterService.update(userId, request);
        return Result.success();
    }

    /**
     * 删除章节
     */
    @PostMapping("/delete/{chapterId}")
    public Result<Void> delete(@PathVariable Long chapterId) {
        Long userId = StpUtil.getLoginIdAsLong();
        chapterService.delete(userId, chapterId);
        return Result.success();
    }

    /**
     * 获取生成上下文（调试/预览用）
     */
    @GetMapping("/context")
    public Result<ChapterContextVO> getContext(
            @RequestParam Long projectId,
            @RequestParam Long outlineId) {
        Long userId = StpUtil.getLoginIdAsLong();
        ChapterContextVO context = chapterService.getGenerationContext(userId, projectId, outlineId);
        return Result.success(context);
    }

    /**
     * 获取生成任务状态
     */
    @GetMapping("/task/{taskId}")
    public Result<GenerationTaskVO> getTask(@PathVariable Long taskId) {
        Long userId = StpUtil.getLoginIdAsLong();
        GenerationTaskVO task = taskService.getTask(userId, taskId);
        return Result.success(task);
    }

    /**
     * 获取用户进行中的任务
     */
    @GetMapping("/tasks/running")
    public Result<List<GenerationTaskVO>> getRunningTasks() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<GenerationTaskVO> tasks = taskService.getRunningTasks(userId);
        return Result.success(tasks);
    }

    /**
     * 取消任务
     */
    @PostMapping("/task/cancel/{taskId}")
    public Result<Void> cancelTask(@PathVariable Long taskId) {
        Long userId = StpUtil.getLoginIdAsLong();
        taskService.cancelTask(userId, taskId);
        return Result.success();
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     */
    private String escapeJson(String content) {
        if (content == null) {
            return "";
        }
        return content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
