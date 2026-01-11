package com.dpbug.server.service.novel.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dpbug.common.constant.NovelConstants;
import com.dpbug.common.enums.ResultCode;
import com.dpbug.common.exception.BusinessException;
import com.dpbug.server.ai.ChatClientFactory;
import com.dpbug.server.mapper.novel.ChapterMapper;
import com.dpbug.server.model.dto.novel.OutlineExpandApplyRequest;
import com.dpbug.server.model.dto.novel.OutlineExpandRequest;
import com.dpbug.server.model.entity.novel.NovelChapter;
import com.dpbug.server.model.entity.novel.NovelProject;
import com.dpbug.server.model.vo.novel.CharacterVO;
import com.dpbug.server.model.vo.novel.ChapterVO;
import com.dpbug.server.model.vo.novel.OutlineVO;
import com.dpbug.server.service.novel.CharacterService;
import com.dpbug.server.service.novel.OutlineService;
import com.dpbug.server.service.novel.PlotExpansionService;
import com.dpbug.server.service.novel.ProjectService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 大纲展开服务实现类
 *
 * @author dpbug
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlotExpansionServiceImpl implements PlotExpansionService {

    private final ChapterMapper chapterMapper;
    private final ProjectService projectService;
    private final OutlineService outlineService;
    private final CharacterService characterService;
    private final ChatClientFactory chatClientFactory;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 展开锁前缀
     */
    private static final String EXPAND_LOCK_PREFIX = "outline:expand:";

    /**
     * 锁超时时间（秒）
     */
    private static final long LOCK_TIMEOUT_SECONDS = 300;

    @Override
    public Flux<String> previewExpansion(Long userId, OutlineExpandRequest request) {
        // 获取大纲并检查权限
        OutlineVO outline = outlineService.getById(userId, request.getOutlineId());
        NovelProject project = projectService.checkOwnership(userId, outline.getProjectId());

        // 获取分布式锁
        String lockKey = EXPAND_LOCK_PREFIX + project.getId() + ":" + outline.getId();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该大纲正在展开中，请稍后再试");
        }

        // 使用 Sinks 创建消息流
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        try {
            // 获取角色信息
            List<CharacterVO> characters = characterService.listByProjectInternal(project.getId());

            // 构建提示词
            String prompt = buildExpansionPrompt(project, outline, characters, request);

            // 创建 ChatClient
            ChatClient chatClient = chatClientFactory.createForComplexTask(userId);

            // 发送进度消息
            emitProgress(sink, "准备展开参数...", 10);

            // 存储完整响应
            StringBuilder fullResponse = new StringBuilder();

            // 发送进度消息
            emitProgress(sink, "AI正在生成章节规划...", 30);

            // 流式调用 AI
            chatClient.prompt()
                    .user(prompt)
                    .stream()
                    .content()
                    .doOnNext(chunk -> {
                        fullResponse.append(chunk);
                        // 可以选择性地发送 chunk 消息
                    })
                    .doOnComplete(() -> {
                        try {
                            // 发送进度消息
                            emitProgress(sink, "解析生成结果...", 80);

                            // 解析 JSON 响应
                            String jsonResponse = extractJsonFromResponse(fullResponse.toString());
                            List<Map<String, Object>> chapterPlans = parseChapterPlans(jsonResponse);

                            // 构建结果
                            Map<String, Object> result = new HashMap<>();
                            result.put("outlineId", outline.getId());
                            result.put("outlineTitle", outline.getTitle());
                            result.put("outlineContent", outline.getContent());
                            result.put("chapterPlans", chapterPlans);

                            // 发送结果消息
                            emitResult(sink, result);

                            // 发送完成消息
                            emitDone(sink);
                        } catch (Exception e) {
                            log.error("解析展开结果失败", e);
                            emitError(sink, "解析展开结果失败: " + e.getMessage());
                        } finally {
                            // 释放锁
                            redisTemplate.delete(lockKey);
                        }
                    })
                    .doOnError(error -> {
                        log.error("AI展开失败", error);
                        emitError(sink, "AI展开失败: " + error.getMessage());
                        // 释放锁
                        redisTemplate.delete(lockKey);
                    })
                    .subscribe();

            return sink.asFlux();
        } catch (Exception e) {
            // 释放锁
            redisTemplate.delete(lockKey);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChapterVO> applyExpansion(Long userId, OutlineExpandApplyRequest request) {
        // 获取大纲并检查权限
        OutlineVO outline = outlineService.getById(userId, request.getOutlineId());
        NovelProject project = projectService.checkOwnership(userId, outline.getProjectId());

        // 检查是否已展开
        if (isExpanded(request.getOutlineId())) {
            if (Boolean.TRUE.equals(request.getForce())) {
                // 强制覆盖：删除已有章节
                deleteExpandedChapters(userId, request.getOutlineId());
            } else {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "该大纲已展开，如需重新展开请设置force=true");
            }
        }

        // 计算起始章节号
        int startChapterNumber = calculateStartChapterNumber(project.getId(), outline.getOrderIndex());

        // 创建章节记录
        List<ChapterVO> createdChapters = new ArrayList<>();
        List<OutlineExpandApplyRequest.ChapterPlanDTO> plans = request.getChapterPlans();

        for (int i = 0; i < plans.size(); i++) {
            OutlineExpandApplyRequest.ChapterPlanDTO plan = plans.get(i);

            NovelChapter chapter = new NovelChapter();
            chapter.setProjectId(project.getId());
            chapter.setOutlineId(outline.getId());
            chapter.setChapterNumber(startChapterNumber + i);
            chapter.setSubIndex(plan.getSubIndex() != null ? plan.getSubIndex() : i + 1);
            chapter.setTitle(plan.getTitle());
            chapter.setSummary(plan.getPlotSummary());
            chapter.setStatus(NovelConstants.ChapterStatus.DRAFT);
            chapter.setGenerationStatus(NovelConstants.GenerationStatus.PENDING);
            chapter.setVersion(1);

            // 构建 expansionPlan
            Map<String, Object> expansionPlan = buildExpansionPlanMap(plan);
            chapter.setExpansionPlan(expansionPlan);

            chapterMapper.insert(chapter);

            ChapterVO vo = new ChapterVO();
            BeanUtils.copyProperties(chapter, vo);
            createdChapters.add(vo);
        }

        log.info("应用展开成功: userId={}, outlineId={}, chapterCount={}",
                userId, request.getOutlineId(), createdChapters.size());

        return createdChapters;
    }

    @Override
    public List<ChapterVO> getExpandedChapters(Long userId, Long outlineId) {
        // 获取大纲并检查权限（权限检查会抛出异常）
        outlineService.getById(userId, outlineId);

        // 查询该大纲下的所有章节
        LambdaQueryWrapper<NovelChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelChapter::getOutlineId, outlineId)
                .orderByAsc(NovelChapter::getSubIndex);

        List<NovelChapter> chapters = chapterMapper.selectList(wrapper);

        return chapters.stream()
                .map(this::convertToVO)
                .toList();
    }

    @Override
    public boolean isExpanded(Long outlineId) {
        LambdaQueryWrapper<NovelChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelChapter::getOutlineId, outlineId);
        return chapterMapper.selectCount(wrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteExpandedChapters(Long userId, Long outlineId) {
        // 获取大纲并检查权限（权限检查会抛出异常）
        outlineService.getById(userId, outlineId);

        // 删除该大纲下的所有章节
        LambdaQueryWrapper<NovelChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelChapter::getOutlineId, outlineId);
        chapterMapper.delete(wrapper);

        log.info("删除展开章节: userId={}, outlineId={}", userId, outlineId);
    }

    /**
     * 构建展开提示词
     */
    private String buildExpansionPrompt(NovelProject project, OutlineVO outline,
                                         List<CharacterVO> characters, OutlineExpandRequest request) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是一位专业的小说编剧。请将以下大纲节点展开为").append(request.getTargetChapterCount()).append("个详细的子章节规划。\n\n");

        // 项目信息
        sb.append("# 项目信息\n");
        sb.append("书名: ").append(project.getTitle()).append("\n");
        sb.append("类型: ").append(nullToEmpty(project.getGenre())).append("\n");
        sb.append("主题: ").append(nullToEmpty(project.getTheme())).append("\n\n");

        // 世界观
        sb.append("# 世界观设定\n");
        sb.append("时间背景: ").append(nullToEmpty(project.getWorldTimePeriod())).append("\n");
        sb.append("地理位置: ").append(nullToEmpty(project.getWorldLocation())).append("\n");
        sb.append("氛围基调: ").append(nullToEmpty(project.getWorldAtmosphere())).append("\n");
        sb.append("世界规则: ").append(nullToEmpty(project.getWorldRules())).append("\n\n");

        // 角色信息
        if (characters != null && !characters.isEmpty()) {
            sb.append("# 主要角色\n");
            for (CharacterVO c : characters) {
                if (!Integer.valueOf(1).equals(c.getIsOrganization())) {
                    sb.append("- ").append(c.getName());
                    if (c.getRoleType() != null) {
                        sb.append("(").append(c.getRoleType()).append(")");
                    }
                    sb.append(": ").append(nullToEmpty(c.getPersonality())).append("\n");
                }
            }
            sb.append("\n");
        }

        // 当前大纲
        sb.append("# 待展开的大纲节点\n");
        sb.append("标题: ").append(outline.getTitle()).append("\n");
        sb.append("内容: ").append(nullToEmpty(outline.getContent())).append("\n\n");

        // 展开策略说明
        sb.append("# 展开策略\n");
        String strategy = request.getStrategy();
        if ("climax".equals(strategy)) {
            sb.append("采用高潮集中策略: 前几章铺垫，后几章集中释放戏剧张力\n");
        } else if ("detail".equals(strategy)) {
            sb.append("采用细节展开策略: 充分展开每个场景，注重描写和氛围\n");
        } else {
            sb.append("采用均衡分布策略: 情节节奏均匀，各章节戏剧张力相近\n");
        }
        sb.append("\n");

        // 自定义要求
        if (request.getCustomRequirements() != null && !request.getCustomRequirements().isBlank()) {
            sb.append("# 额外要求\n");
            sb.append(request.getCustomRequirements()).append("\n\n");
        }

        // 输出格式
        sb.append("# 输出要求\n");
        sb.append("请生成").append(request.getTargetChapterCount()).append("个子章节的详细规划，每个规划包含:\n");
        sb.append("- subIndex: 子章节序号(从1开始)\n");
        sb.append("- title: 章节标题\n");
        sb.append("- plotSummary: 本章剧情摘要(100-200字)\n");
        sb.append("- keyEvents: 关键事件列表\n");
        sb.append("- characterFocus: 本章聚焦的角色\n");
        sb.append("- emotionalTone: 情绪基调\n");
        sb.append("- narrativeGoal: 叙事目标\n");
        sb.append("- conflictType: 冲突类型(外部冲突/内部冲突/悬疑等)\n");
        sb.append("- estimatedWords: 预估字数(建议3000-5000)\n");

        if (Boolean.TRUE.equals(request.getEnableSceneAnalysis())) {
            sb.append("- scenes: 场景列表,每个场景包含location(地点)、characters(角色)、purpose(目的)\n");
        }

        sb.append("\n只返回纯JSON数组，不要有```json```标记，不要有其他说明文字。\n");

        return sb.toString();
    }

    /**
     * 计算起始章节号（全局连续）
     */
    private int calculateStartChapterNumber(Long projectId, Integer outlineOrderIndex) {
        // 简化处理：直接返回当前最大章节号 + 1
        Integer maxChapterNumber = chapterMapper.selectMaxChapterNumber(projectId);
        return maxChapterNumber != null ? maxChapterNumber + 1 : 1;
    }

    /**
     * 从响应中提取 JSON
     */
    private String extractJsonFromResponse(String response) {
        String trimmed = response.trim();

        // 移除可能的 markdown 代码块标记
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        return trimmed.trim();
    }

    /**
     * 解析章节规划 JSON
     */
    private List<Map<String, Object>> parseChapterPlans(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析章节规划JSON失败: {}", json, e);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "解析AI响应失败，请重试");
        }
    }

    /**
     * 构建 expansionPlan Map
     */
    private Map<String, Object> buildExpansionPlanMap(OutlineExpandApplyRequest.ChapterPlanDTO plan) {
        Map<String, Object> map = new HashMap<>();
        map.put("plotSummary", plan.getPlotSummary());
        map.put("keyEvents", plan.getKeyEvents());
        map.put("characterFocus", plan.getCharacterFocus());
        map.put("emotionalTone", plan.getEmotionalTone());
        map.put("narrativeGoal", plan.getNarrativeGoal());
        map.put("conflictType", plan.getConflictType());
        map.put("estimatedWords", plan.getEstimatedWords());

        if (plan.getScenes() != null) {
            List<Map<String, Object>> scenes = new ArrayList<>();
            for (OutlineExpandApplyRequest.SceneDTO scene : plan.getScenes()) {
                Map<String, Object> sceneMap = new HashMap<>();
                sceneMap.put("location", scene.getLocation());
                sceneMap.put("characters", scene.getCharacters());
                sceneMap.put("purpose", scene.getPurpose());
                scenes.add(sceneMap);
            }
            map.put("scenes", scenes);
        }

        return map;
    }

    /**
     * 发送进度消息
     */
    private void emitProgress(Sinks.Many<String> sink, String message, int progress) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "progress");
            data.put("message", message);
            data.put("progress", progress);
            sink.tryEmitNext(objectMapper.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            log.error("序列化进度消息失败", e);
        }
    }

    /**
     * 发送结果消息
     */
    private void emitResult(Sinks.Many<String> sink, Map<String, Object> result) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "result");
            data.put("data", result);
            sink.tryEmitNext(objectMapper.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            log.error("序列化结果消息失败", e);
        }
    }

    /**
     * 发送完成消息
     */
    private void emitDone(Sinks.Many<String> sink) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "done");
            sink.tryEmitNext(objectMapper.writeValueAsString(data));
            sink.tryEmitComplete();
        } catch (JsonProcessingException e) {
            log.error("序列化完成消息失败", e);
        }
    }

    /**
     * 发送错误消息
     */
    private void emitError(Sinks.Many<String> sink, String errorMessage) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "error");
            data.put("message", errorMessage);
            sink.tryEmitNext(objectMapper.writeValueAsString(data));
            sink.tryEmitComplete();
        } catch (JsonProcessingException e) {
            log.error("序列化错误消息失败", e);
        }
    }

    private String nullToEmpty(String str) {
        return str != null ? str : "";
    }

    private ChapterVO convertToVO(NovelChapter chapter) {
        ChapterVO vo = new ChapterVO();
        BeanUtils.copyProperties(chapter, vo);
        return vo;
    }
}
