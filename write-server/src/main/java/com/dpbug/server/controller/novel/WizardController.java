package com.dpbug.server.controller.novel;

import cn.dev33.satoken.stp.StpUtil;
import com.dpbug.common.domain.Result;
import com.dpbug.server.model.dto.novel.CharacterGenerateRequest;
import com.dpbug.server.model.dto.novel.OutlineGenerateRequest;
import com.dpbug.server.model.dto.novel.WizardStatusUpdateRequest;
import com.dpbug.server.model.dto.novel.WorldGenerateRequest;
import com.dpbug.server.model.vo.novel.CharacterVO;
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
     * 生成角色（非流式）
     */
    @PostMapping("/characters/generate")
    public Result<List<CharacterVO>> generateCharacters(@RequestBody @Valid CharacterGenerateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<CharacterVO> characters = wizardService.generateCharacters(userId, request);
        return Result.success(characters);
    }

    /**
     * 生成大纲（SSE流式）
     */
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
