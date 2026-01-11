package com.dpbug.server.controller.novel;

import cn.dev33.satoken.stp.StpUtil;
import com.dpbug.common.domain.Result;
import com.dpbug.server.model.dto.novel.CharacterGenerateRequest;
import com.dpbug.server.model.dto.novel.OutlineGenerateRequest;
import com.dpbug.server.model.dto.novel.WizardStatusUpdateRequest;
import com.dpbug.server.model.dto.novel.WorldGenerateRequest;
import com.dpbug.server.model.vo.novel.CharacterVO;
import com.dpbug.server.model.vo.novel.GenerationTaskVO;
import com.dpbug.server.service.novel.GenerationTaskService;
import com.dpbug.server.service.novel.WizardService;
import com.dpbug.server.service.novel.WizardService.WizardProgressVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author dpbug
 * @description
 */
@Slf4j
@RestController
@RequestMapping("/api/novel/wizard")
@RequiredArgsConstructor
public class WizardController {

    private final WizardService wizardService;
    private final GenerationTaskService taskService;

    /**
     * 生成世界观（SSE流式）
     */
    @PostMapping(value = "/world/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generateWorld(@RequestBody @Valid WorldGenerateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        return wizardService.generateWorld(userId, request)
                .map(content -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(content)
                        .build())
                .concatWith(Mono.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build()));
    }

    /**
     * 生成角色（非流式，同步方式 - 已废弃，保留向后兼容）
     * @deprecated 请使用 /characters/generate/async 异步接口
     */
    @Deprecated
    @PostMapping("/characters/generate")
    public Result<List<CharacterVO>> generateCharacters(@RequestBody @Valid CharacterGenerateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<CharacterVO> characters = wizardService.generateCharacters(userId, request);
        return Result.success(characters);
    }

    /**
     * 异步生成角色
     * 返回任务ID，前端通过轮询 /task/{taskId} 获取进度和结果
     */
    @PostMapping("/characters/generate/async")
    public Result<GenerationTaskVO> generateCharactersAsync(@RequestBody @Valid CharacterGenerateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        GenerationTaskVO task = wizardService.submitCharacterGenerationTask(userId, request);
        return Result.success(task);
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/task")
    public Result<GenerationTaskVO> getTask(@RequestParam @NotNull(message = "任务ID不能为空") Long taskId) {
        Long userId = StpUtil.getLoginIdAsLong();
        GenerationTaskVO task = taskService.getTask(userId, taskId);
        return Result.success(task);
    }

    /**
     * 生成大纲（SSE流式 - 已废弃，保留向后兼容）
     * @deprecated 请使用 /outlines/generate/async 异步接口
     */
    @Deprecated
    @PostMapping(value = "/outlines/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generateOutlines(@RequestBody @Valid OutlineGenerateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();

        return wizardService.generateOutlines(userId, request)
                .map(content -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(content)
                        .build())
                .concatWith(Mono.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build()));
    }

    /**
     * 异步生成大纲
     * 返回任务ID，前端通过轮询 /task/{taskId} 获取进度和结果
     */
    @PostMapping("/outlines/generate/async")
    public Result<GenerationTaskVO> generateOutlinesAsync(@RequestBody @Valid OutlineGenerateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        GenerationTaskVO task = wizardService.submitOutlineGenerationTask(userId, request);
        return Result.success(task);
    }

    /**
     * 更新向导状态
     */
    @PostMapping("/status")
    public Result<Void> updateStatus(@RequestBody @Valid WizardStatusUpdateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        wizardService.updateWizardStatus(userId, request.getProjectId(),
                request.getStatus(), request.getStep());
        return Result.success();
    }

    /**
     * 获取向导进度
     */
    @GetMapping("/progress")
    public Result<WizardProgressVO> getProgress(
            @RequestParam @NotNull(message = "项目ID不能为空") Long projectId) {
        Long userId = StpUtil.getLoginIdAsLong();
        WizardProgressVO progress = wizardService.getWizardProgress(userId, projectId);
        return Result.success(progress);
    }

    /**
     * 重置向导步骤
     * 会删除指定步骤及之后的所有生成数据
     */
    @PostMapping("/reset")
    public Result<Void> resetStep(
            @RequestParam @NotNull(message = "项目ID不能为空") Long projectId,
            @RequestParam @NotNull(message = "步骤不能为空") @Min(0) @Max(3) Integer step) {
        Long userId = StpUtil.getLoginIdAsLong();
        wizardService.resetWizardStep(userId, projectId, step);
        return Result.success();
    }
}
