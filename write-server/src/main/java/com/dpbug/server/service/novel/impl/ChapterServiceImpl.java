package com.dpbug.server.service.novel.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dpbug.common.constant.NovelConstants;
import com.dpbug.common.domain.PageResult;
import com.dpbug.common.enums.ResultCode;
import com.dpbug.common.exception.BusinessException;
import com.dpbug.common.utils.Assert;
import com.dpbug.server.ai.ChatClientFactory;
import com.dpbug.server.ai.prompt.PromptTemplates;
import com.dpbug.server.mapper.novel.ChapterMapper;
import com.dpbug.server.model.dto.novel.BatchGenerateRequest;
import com.dpbug.server.model.dto.novel.ChapterGenerateRequest;
import com.dpbug.server.model.dto.novel.ChapterPolishRequest;
import com.dpbug.server.model.dto.novel.ChapterQueryRequest;
import com.dpbug.server.model.dto.novel.ChapterUpdateRequest;
import com.dpbug.server.model.entity.novel.NovelChapter;
import com.dpbug.server.model.entity.novel.NovelProject;
import com.dpbug.server.model.vo.novel.ChapterContextVO;
import com.dpbug.server.model.vo.novel.ChapterDetailVO;
import com.dpbug.server.model.vo.novel.ChapterSummaryVO;
import com.dpbug.server.model.vo.novel.ChapterVO;
import com.dpbug.server.model.vo.novel.CharacterVO;
import com.dpbug.server.model.vo.novel.OutlineVO;
import com.dpbug.server.model.vo.novel.StoryMemoryVO;
import com.dpbug.server.service.novel.ChapterAsyncService;
import com.dpbug.server.service.novel.ChapterContextBuilder;
import com.dpbug.server.service.novel.ChapterService;
import com.dpbug.server.service.novel.GenerationTaskService;
import com.dpbug.server.service.novel.OutlineService;
import com.dpbug.server.service.novel.ProjectService;
import com.dpbug.server.service.novel.StoryMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 章节服务实现类
 *
 * @author dpbug
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private final ChapterMapper chapterMapper;
    private final ProjectService projectService;
    private final OutlineService outlineService;
    private final StoryMemoryService storyMemoryService;
    private final GenerationTaskService taskService;
    private final ChapterContextBuilder contextBuilder;
    private final ChapterAsyncService chapterAsyncService;
    private final ChatClientFactory chatClientFactory;

    /**
     * 章节生成锁，防止同一大纲同时生成多个章节
     * Key: projectId:outlineId:subIndex
     */
    private static final ConcurrentHashMap<String, Boolean> GENERATION_LOCKS = new ConcurrentHashMap<>();

    @Override
    public Flux<String> generateChapter(Long userId, ChapterGenerateRequest request) {
        // 获取大纲及项目信息
        OutlineVO outline = outlineService.getByIdInternal(request.getOutlineId());

        NovelProject project = projectService.checkOwnership(userId, outline.getProjectId());

        // 规范化 subIndex（one-to-one 默认 0；one-to-many 从 1 开始）
        Integer normalizedSubIndex = request.getSubIndex() != null ? request.getSubIndex() : 0;
        request.setSubIndex(normalizedSubIndex);

        // 并发控制：检查是否有正在生成的章节
        String lockKey = buildLockKey(project.getId(), outline.getId(), normalizedSubIndex);
        if (GENERATION_LOCKS.putIfAbsent(lockKey, Boolean.TRUE) != null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该章节正在生成中，请稍后再试");
        }

        NovelChapter chapter = null;
        boolean reuseExisting = false;
        Integer chapterNumber = null;
        try {
            // one-to-many：优先复用“展开(apply)后已创建的待生成章节记录”，避免重复建章
            if (normalizedSubIndex > 0) {
                NovelChapter existing = chapterMapper.selectLatestByOutlineSubIndex(
                        project.getId(), outline.getId(), normalizedSubIndex);
                if (existing != null) {
                    String generationStatus = existing.getGenerationStatus();
                    if (NovelConstants.GenerationStatus.GENERATING.equals(generationStatus)) {
                        throw new BusinessException(ResultCode.BUSINESS_ERROR, "该章节正在生成中，请稍后再试");
                    }
                    if (NovelConstants.GenerationStatus.COMPLETED.equals(generationStatus)) {
                        throw new BusinessException(ResultCode.BUSINESS_ERROR, "该章节已生成完成，如需重写请使用“重新生成”功能");
                    }

                    // 更新为 generating，并同步本次生成参数（不改 chapterNumber/title/subIndex/expansionPlan）
                    NovelChapter update = new NovelChapter();
                    update.setId(existing.getId());
                    update.setGenerationStatus(NovelConstants.GenerationStatus.GENERATING);
                    update.setStyleCode(request.getStyleCode() != null ? request.getStyleCode() : existing.getStyleCode());

                    Map<String, Object> params = new HashMap<>();
                    params.put("temperature", request.getTemperature() != null ? request.getTemperature() : 0.7);
                    params.put("topP", request.getTopP() != null ? request.getTopP() : 0.9);
                    params.put("targetWordCount", request.getTargetWordCount() != null ?
                            request.getTargetWordCount() : NovelConstants.ChapterConfig.DEFAULT_WORD_COUNT);
                    update.setGenerationParams(params);

                    chapterMapper.updateById(update);

                    chapter = existing;
                    chapterNumber = existing.getChapterNumber();
                    reuseExisting = true;
                    log.info("复用已展开章节记录并开始生成: chapterId={}, chapterNumber={}, outlineId={}, subIndex={}",
                            chapter.getId(), chapterNumber, outline.getId(), normalizedSubIndex);
                }
            }

            // one-to-one 或未找到可复用记录：创建新章节记录(状态:generating)
            if (chapter == null) {
                // 计算章节号
                chapterNumber = calculateChapterNumber(project.getId(), outline, normalizedSubIndex);
                chapter = createChapterRecord(project, outline, chapterNumber, request);
            }
            final Long chapterId = chapter.getId();
            final Integer finalChapterNumber = chapterNumber;
            final boolean finalReuseExisting = reuseExisting;

            ChapterContextVO context = contextBuilder.buildContext(userId, project, outline, finalChapterNumber, request);

            String systemPrompt = buildSystemPrompt(context, request);
            String userPrompt = buildUserPrompt(context, request);

            ChatClient chatClient = chatClientFactory.createForUser(userId);
            AtomicReference<String> fullContent = new AtomicReference<>("");

            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .stream()
                    .content()
                    .doOnNext(chunk -> fullContent.updateAndGet(current -> current + chunk))
                    .doOnComplete(() -> {
                        try {
                            // 保存章节内容
                            String modelName = chatClientFactory.getCurrentModelName(userId);
                            saveChapterContent(chapterId, fullContent.get(), modelName);

                            // 通过异步服务提取记忆和生成摘要
                            chapterAsyncService.asyncExtractMemories(userId, project.getId(), chapterId,
                                    finalChapterNumber, fullContent.get());
                            chapterAsyncService.asyncGenerateSummary(chapterId, fullContent.get());

                            // 更新项目统计
                            updateProjectStatistics(project);
                        } finally {
                            // 释放锁
                            GENERATION_LOCKS.remove(lockKey);
                        }
                    })
                    .doOnError(error -> {
                        // 释放锁
                        GENERATION_LOCKS.remove(lockKey);
                        if (finalReuseExisting) {
                            updateChapterStatus(chapterId, NovelConstants.GenerationStatus.FAILED);
                            log.error("章节生成失败，已标记章节为FAILED: chapterId={}", chapterId, error);
                        } else {
                            // 删除已创建的章节记录（因为生成失败了）
                            chapterMapper.deleteById(chapterId);
                            log.error("章节生成失败，已删除章节记录: chapterId={}", chapterId, error);
                        }
                    })
                    .doOnCancel(() -> {
                        // 取消时也释放锁，并处理未完成的章节记录
                        GENERATION_LOCKS.remove(lockKey);
                        if (finalReuseExisting) {
                            updateChapterStatus(chapterId, NovelConstants.GenerationStatus.FAILED);
                            log.info("章节生成被取消，已标记章节为FAILED: chapterId={}", chapterId);
                        } else {
                            chapterMapper.deleteById(chapterId);
                            log.info("章节生成被取消，已删除章节记录: chapterId={}", chapterId);
                        }
                    });
        } catch (Exception e) {
            // 异常时释放锁，并删除已创建的章节记录
            GENERATION_LOCKS.remove(lockKey);
            if (chapter != null) {
                if (reuseExisting) {
                    updateChapterStatus(chapter.getId(), NovelConstants.GenerationStatus.FAILED);
                    log.warn("章节生成准备阶段异常，已标记章节为FAILED: chapterId={}", chapter.getId());
                } else {
                    chapterMapper.deleteById(chapter.getId());
                    log.warn("章节生成准备阶段异常，已删除章节记录: chapterId={}", chapter.getId());
                }
            }
            throw e;
        }
    }

    /**
     * 构建锁的Key
     */
    private String buildLockKey(Long projectId, Long outlineId, Integer subIndex) {
        return String.format("%d:%d:%d", projectId, outlineId, subIndex != null ? subIndex : 0);
    }

    /**
     * 计算章节号（全局连续递增策略）
     * <p>
     * 无论是 one-to-one 还是 one-to-many 模式，章节号都采用全局连续递增。
     * - 新章节的编号 = 当前项目最大章节号 + 1
     * - 如果项目没有章节，从 1 开始
     * </p>
     */
    private Integer calculateChapterNumber(Long projectId, OutlineVO outline, Integer subIndex) {
        // 查询当前项目的最大章节号
        Integer maxChapterNumber = chapterMapper.selectMaxChapterNumber(projectId);

        // 新章节号 = 最大章节号 + 1，如果没有章节则从 1 开始
        return maxChapterNumber != null ? maxChapterNumber + 1 : 1;
    }

    /**
     * 创建章节记录
     */
    private NovelChapter createChapterRecord(NovelProject project, OutlineVO outline,
                                              Integer chapterNumber, ChapterGenerateRequest request) {
        NovelChapter chapter = new NovelChapter();
        chapter.setProjectId(project.getId());
        chapter.setOutlineId(outline.getId());
        chapter.setChapterNumber(chapterNumber);
        chapter.setSubIndex(request.getSubIndex() != null ? request.getSubIndex() : 0);
        chapter.setTitle(outline.getTitle());
        chapter.setStatus(NovelConstants.ChapterStatus.DRAFT);
        chapter.setGenerationStatus(NovelConstants.GenerationStatus.GENERATING);
        chapter.setStyleCode(request.getStyleCode());
        chapter.setVersion(1);

        // 保存生成参数
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", request.getTemperature() != null ? request.getTemperature() : 0.7);
        params.put("topP", request.getTopP() != null ? request.getTopP() : 0.9);
        params.put("targetWordCount", request.getTargetWordCount() != null ?
                request.getTargetWordCount() : NovelConstants.ChapterConfig.DEFAULT_WORD_COUNT);
        chapter.setGenerationParams(params);

        chapterMapper.insert(chapter);
        log.info("创建章节记录: chapterId={}, chapterNumber={}", chapter.getId(), chapterNumber);
        return chapter;
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(ChapterContextVO context, ChapterGenerateRequest request) {
        StringBuilder sb = new StringBuilder();

        // 基础角色设定
        sb.append("你是一位专业的小说作家,擅长根据大纲创作引人入胜的章节内容。\n\n");

        // 写作风格
        if (context.getWritingStylePrompt() != null) {
            sb.append("# 写作风格要求\n");
            sb.append(context.getWritingStylePrompt());
            sb.append("\n\n");
        }

        // 叙事人称
        String perspective = request.getNarrativePerspective();
        if (perspective != null && !perspective.isBlank()) {
            sb.append("# 叙事人称\n");
            sb.append("请使用").append(perspective).append("进行叙述。\n\n");
        }

        return sb.toString();
    }

    /**
     * 构建用户提示词
     */
    private String buildUserPrompt(ChapterContextVO context, ChapterGenerateRequest request) {
        StringBuilder sb = new StringBuilder();

        // 项目信息
        sb.append("# 小说信息\n");
        sb.append("书名:").append(context.getProjectTitle()).append("\n");
        sb.append("类型:").append(context.getGenre()).append("\n");
        sb.append("主题:").append(context.getTheme()).append("\n\n");

        // 世界观
        sb.append("# 世界观设定\n");
        sb.append("时间背景:").append(nullToEmpty(context.getWorldTimePeriod())).append("\n");
        sb.append("地理位置:").append(nullToEmpty(context.getWorldLocation())).append("\n");
        sb.append("氛围基调:").append(nullToEmpty(context.getWorldAtmosphere())).append("\n");
        sb.append("世界规则:").append(nullToEmpty(context.getWorldRules())).append("\n\n");

        // 主要角色
        if (context.getMainCharacters() != null && !context.getMainCharacters().isEmpty()) {
            sb.append("# 主要角色\n");
            for (CharacterVO c : context.getMainCharacters()) {
                sb.append("- ").append(c.getName());
                if (c.getRoleType() != null) {
                    sb.append("(").append(c.getRoleType()).append(")");
                }
                sb.append(":").append(nullToEmpty(c.getBackground())).append("\n");
            }
            sb.append("\n");
        }

        // 历史章节上下文
        if (context.getRecentChapters() != null && !context.getRecentChapters().isEmpty()) {
            sb.append("# 前情回顾\n");
            for (ChapterSummaryVO c : context.getRecentChapters()) {
                sb.append("第").append(c.getChapterNumber()).append("章 ");
                sb.append(c.getTitle()).append(":");
                sb.append(nullToEmpty(c.getSummary())).append("\n");
            }
            sb.append("\n");
        }

        // RAG记忆
        if (context.getRelatedMemories() != null && !context.getRelatedMemories().isEmpty()) {
            sb.append("# 相关情节记忆\n");
            for (StoryMemoryVO m : context.getRelatedMemories()) {
                sb.append("- ").append(m.getTitle()).append(":").append(m.getContent()).append("\n");
            }
            sb.append("\n");
        }

        // 未完结伏笔
        if (context.getPendingForeshadows() != null && !context.getPendingForeshadows().isEmpty()) {
            sb.append("# 待回收的伏笔\n");
            for (StoryMemoryVO f : context.getPendingForeshadows()) {
                sb.append("- ").append(f.getTitle()).append("(第").append(f.getStoryTimeline()).append("章埋下)\n");
            }
            sb.append("\n");
        }

        // 当前章节要求
        sb.append("# 本章任务\n");
        OutlineVO currentOutline = context.getCurrentOutline();
        if (currentOutline != null) {
            sb.append("章节标题:").append(nullToEmpty(currentOutline.getTitle())).append("\n");
            sb.append("大纲内容:").append(nullToEmpty(currentOutline.getContent())).append("\n");
        }

        // 展开规划（one-to-many模式专用）
        if (context.getExpansionPlan() != null && !context.getExpansionPlan().isEmpty()) {
            sb.append("\n# 本章详细规划（重要）\n");
            Map<String, Object> plan = context.getExpansionPlan();

            if (plan.get("plotSummary") != null) {
                sb.append("剧情摘要:").append(plan.get("plotSummary")).append("\n");
            }
            if (plan.get("keyEvents") instanceof List<?> events && !events.isEmpty()) {
                sb.append("关键事件:\n");
                for (Object event : events) {
                    sb.append("  - ").append(event).append("\n");
                }
            }
            if (plan.get("characterFocus") instanceof List<?> characters && !characters.isEmpty()) {
                sb.append("聚焦角色:").append(String.join("、", characters.stream()
                        .map(Object::toString).toList())).append("\n");
            }
            if (plan.get("emotionalTone") != null) {
                sb.append("情绪基调:").append(plan.get("emotionalTone")).append("\n");
            }
            if (plan.get("narrativeGoal") != null) {
                sb.append("叙事目标:").append(plan.get("narrativeGoal")).append("\n");
            }
            if (plan.get("conflictType") != null) {
                sb.append("冲突类型:").append(plan.get("conflictType")).append("\n");
            }
            if (plan.get("scenes") instanceof List<?> scenes && !scenes.isEmpty()) {
                sb.append("场景规划:\n");
                for (Object scene : scenes) {
                    if (scene instanceof Map<?, ?> sceneMap) {
                        sb.append("  - 地点:").append(sceneMap.get("location"));
                        if (sceneMap.get("purpose") != null) {
                            sb.append(" | 目的:").append(sceneMap.get("purpose"));
                        }
                        sb.append("\n");
                    }
                }
            }
            sb.append("\n请严格按照以上详细规划创作本章内容，确保覆盖所有关键事件。\n");
        }

        Integer targetWordCount = request.getTargetWordCount() != null ?
                request.getTargetWordCount() : NovelConstants.ChapterConfig.DEFAULT_WORD_COUNT;
        sb.append("目标字数:约").append(targetWordCount).append("字\n\n");

        // 自定义要求
        if (request.getCustomRequirements() != null && !request.getCustomRequirements().isBlank()) {
            sb.append("# 额外要求\n");
            sb.append(request.getCustomRequirements()).append("\n\n");
        }

        sb.append("请根据以上信息,创作本章内容。直接输出章节正文,无需输出标题。");

        return sb.toString();
    }

    private String nullToEmpty(String str) {
        return str != null ? str : "";
    }

    /**
     * 保存章节内容
     */
    private void saveChapterContent(Long chapterId, String content, String aiModel) {
        NovelChapter chapter = new NovelChapter();
        chapter.setId(chapterId);
        chapter.setContent(content);
        chapter.setWordCount(content.length());
        chapter.setGenerationStatus(NovelConstants.GenerationStatus.COMPLETED);
        chapter.setAiModel(aiModel);
        chapterMapper.updateById(chapter);

        log.info("保存章节内容: chapterId={}, wordCount={}", chapterId, content.length());
    }

    /**
     * 更新章节状态
     */
    private void updateChapterStatus(Long chapterId, String status) {
        NovelChapter chapter = new NovelChapter();
        chapter.setId(chapterId);
        chapter.setGenerationStatus(status);
        chapterMapper.updateById(chapter);
    }

    /**
     * 更新项目统计和状态
     * <p>
     * 当项目有章节生成完成时，自动将项目状态从"规划中"改为"创作中"
     */
    private void updateProjectStatistics(NovelProject project) {
        Long projectId = project.getId();
        projectService.refreshStatistics(projectId);

        // 检查并更新项目状态：如果项目处于"规划中"状态，则更新为"创作中"
        if (NovelConstants.ProjectStatus.PLANNING.equals(project.getStatus())) {
            projectService.updateProjectStatus(projectId, NovelConstants.ProjectStatus.WRITING);
        }

        log.info("更新项目统计: projectId={}", projectId);
    }

    @Override
    public Long batchGenerate(Long userId, BatchGenerateRequest request) {
        // 检查项目权限
        NovelProject project = projectService.checkOwnership(userId, request.getProjectId());

        // 创建批量任务
        Map<String, Object> params = new HashMap<>();
        params.put("outlineIds", request.getOutlineIds());
        params.put("styleCode", request.getStyleCode());
        params.put("targetWordCount", request.getTargetWordCount());
        params.put("enableAnalysis", request.getEnableAnalysis());

        var task = taskService.createTask(userId, project.getId(),
                NovelConstants.TaskType.BATCH_CHAPTER, params);

        // 异步执行批量生成
        chapterAsyncService.asyncBatchGenerate(userId, task, request);

        log.info("创建批量生成任务: userId={}, projectId={}, taskId={}, count={}",
                userId, project.getId(), task.getId(), request.getOutlineIds().size());

        return task.getId();
    }

    @Override
    public Flux<String> regenerateChapter(Long userId, Long chapterId, ChapterGenerateRequest request) {
        // 获取章节和项目
        ChapterWithProject chapterWithProject = getChapterWithProject(userId, chapterId);
        NovelChapter existingChapter = chapterWithProject.chapter();
        NovelProject project = chapterWithProject.project();

        // 获取大纲
        OutlineVO outline = outlineService.getByIdInternal(existingChapter.getOutlineId());

        // 创建新版本记录
        NovelChapter newChapter = new NovelChapter();
        BeanUtils.copyProperties(existingChapter, newChapter);
        newChapter.setId(null);
        newChapter.setContent(null);
        newChapter.setSummary(null);
        newChapter.setVersion(existingChapter.getVersion() + 1);
        newChapter.setPreviousVersionId(existingChapter.getId());
        newChapter.setGenerationStatus(NovelConstants.GenerationStatus.GENERATING);
        newChapter.setStyleCode(request.getStyleCode() != null ? request.getStyleCode() : existingChapter.getStyleCode());

        chapterMapper.insert(newChapter);
        log.info("重新生成章节: oldChapterId={}, newChapterId={}, version={}",
                chapterId, newChapter.getId(), newChapter.getVersion());

        // 构建上下文
        ChapterContextVO context = contextBuilder.buildContext(userId, project, outline,
                newChapter.getChapterNumber(), request);

        // 构建提示词
        String systemPrompt = buildSystemPrompt(context, request);
        String userPrompt = buildUserPrompt(context, request);

        // 创建ChatClient并流式生成
        ChatClient chatClient = chatClientFactory.createForUser(userId);
        AtomicReference<String> fullContent = new AtomicReference<>("");

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
                .doOnNext(chunk -> fullContent.updateAndGet(current -> current + chunk))
                .doOnComplete(() -> {
                    String modelName = chatClientFactory.getCurrentModelName(userId);
                    saveChapterContent(newChapter.getId(), fullContent.get(), modelName);
                    chapterAsyncService.asyncExtractMemories(userId, project.getId(), newChapter.getId(),
                            newChapter.getChapterNumber(), fullContent.get());
                    chapterAsyncService.asyncGenerateSummary(newChapter.getId(), fullContent.get());
                    updateProjectStatistics(project);
                })
                .doOnError(error -> {
                    updateChapterStatus(newChapter.getId(), NovelConstants.GenerationStatus.FAILED);
                    log.error("重新生成章节失败: chapterId={}", newChapter.getId(), error);
                });
    }

    @Override
    public Flux<String> polishChapter(Long userId, ChapterPolishRequest request) {
        // 检查章节权限
        NovelChapter chapter = getChapterWithPermissionCheck(userId, request.getChapterId());

        // 检查章节是否有内容
        Assert.isTrue(chapter.getContent() != null && !chapter.getContent().isBlank(), "章节内容为空，无法润色");

        log.info("润色章节: userId={}, chapterId={}, polishType={}",
                userId, request.getChapterId(), request.getPolishType());

        // 构建润色提示词
        String polishPrompt = buildPolishPrompt(chapter.getContent(), request);

        // 创建ChatClient并流式生成
        ChatClient chatClient = chatClientFactory.createForUser(userId);
        AtomicReference<String> fullContent = new AtomicReference<>("");

        return chatClient.prompt()
                .user(polishPrompt)
                .stream()
                .content()
                .doOnNext(chunk -> fullContent.updateAndGet(current -> current + chunk))
                .doOnComplete(() -> {
                    // 更新章节内容
                    String polishedContent = fullContent.get();
                    NovelChapter updateChapter = new NovelChapter();
                    updateChapter.setId(chapter.getId());
                    updateChapter.setContent(polishedContent);
                    updateChapter.setWordCount(polishedContent.length());
                    chapterMapper.updateById(updateChapter);

                    // 异步生成新摘要
                    chapterAsyncService.asyncGenerateSummary(chapter.getId(), polishedContent);

                    log.info("润色完成: chapterId={}, newWordCount={}", chapter.getId(), polishedContent.length());
                })
                .doOnError(error -> {
                    log.error("润色章节失败: chapterId={}", chapter.getId(), error);
                });
    }

    /**
     * 构建润色提示词
     */
    private String buildPolishPrompt(String originalContent, ChapterPolishRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("请对以下小说章节内容进行润色。\n\n");

        // 根据润色类型添加不同的指令
        String polishType = request.getPolishType();
        if (polishType != null) {
            switch (polishType) {
                case "enhance_description" -> sb.append("润色要求：增强环境描写和人物刻画，使场景更加生动。\n");
                case "fix_grammar" -> sb.append("润色要求：修正语法错误和不通顺的表达，保持原意。\n");
                case "adjust_pacing" -> sb.append("润色要求：调整叙事节奏，使情节更加紧凑流畅。\n");
                case "all" -> sb.append("润色要求：全面提升文章质量，包括描写、语法、节奏等方面。\n");
                default -> sb.append("润色要求：优化文章表达，提升可读性。\n");
            }
        } else {
            sb.append("润色要求：优化文章表达，提升可读性。\n");
        }

        // 自定义要求
        if (request.getCustomInstructions() != null && !request.getCustomInstructions().isBlank()) {
            sb.append("额外要求：").append(request.getCustomInstructions()).append("\n");
        }

        sb.append("\n原文内容：\n");
        sb.append(originalContent);
        sb.append("\n\n请直接输出润色后的内容，不要添加任何解释说明。");

        return sb.toString();
    }

    @Override
    public ChapterDetailVO getDetail(Long userId, Long chapterId) {
        NovelChapter chapter = getChapterWithPermissionCheck(userId, chapterId);

        ChapterDetailVO vo = new ChapterDetailVO();
        BeanUtils.copyProperties(chapter, vo);

        // 获取大纲信息（权限已校验）
        if (chapter.getOutlineId() != null) {
            var outline = outlineService.getByIdInternal(chapter.getOutlineId());
            vo.setOutlineTitle(outline.getTitle());
            vo.setOutlineContent(outline.getContent());
        }

        return vo;
    }

    @Override
    public PageResult<ChapterVO> list(Long userId, ChapterQueryRequest pageRequest) {
        // 检查项目权限
        projectService.checkOwnership(userId, pageRequest.getProjectId());

        // 构建查询条件
        LambdaQueryWrapper<NovelChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelChapter::getProjectId, pageRequest.getProjectId())
                .eq(pageRequest.getStatus() != null, NovelChapter::getStatus, pageRequest.getStatus())
                .eq(pageRequest.getGenerationStatus() != null, NovelChapter::getGenerationStatus,
                        pageRequest.getGenerationStatus())
                .eq(pageRequest.getOutlineId() != null, NovelChapter::getOutlineId, pageRequest.getOutlineId())
                .and(pageRequest.getKeyword() != null, w ->
                        w.like(NovelChapter::getTitle, pageRequest.getKeyword())
                                .or()
                                .like(NovelChapter::getContent, pageRequest.getKeyword())
                )
                .orderByAsc(NovelChapter::getChapterNumber, NovelChapter::getSubIndex);

        // 分页查询
        Page<NovelChapter> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        Page<NovelChapter> resultPage = chapterMapper.selectPage(page, wrapper);

        // 转换为VO
        List<ChapterVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();

        return PageResult.of(
                (int) resultPage.getCurrent(),
                (int) resultPage.getSize(),
                resultPage.getTotal(),
                voList
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, ChapterUpdateRequest request) {
        NovelChapter existingChapter = getChapterWithPermissionCheck(userId, request.getId());

        // 检查是否需要创建新版本
        if (Boolean.TRUE.equals(request.getCreateNewVersion())) {
            // 创建新版本
            NovelChapter newChapter = new NovelChapter();
            BeanUtils.copyProperties(existingChapter, newChapter);
            newChapter.setId(null);
            newChapter.setVersion(existingChapter.getVersion() + 1);
            newChapter.setPreviousVersionId(existingChapter.getId());

            // 应用更新
            if (request.getTitle() != null) {
                newChapter.setTitle(request.getTitle());
            }
            if (request.getContent() != null) {
                newChapter.setContent(request.getContent());
                newChapter.setWordCount(request.getContent().length());
            }
            if (request.getStatus() != null) {
                newChapter.setStatus(request.getStatus());
            }

            chapterMapper.insert(newChapter);
            log.info("创建章节新版本: oldId={}, newId={}, version={}",
                    existingChapter.getId(), newChapter.getId(), newChapter.getVersion());
        } else {
            // 直接更新
            NovelChapter updateChapter = new NovelChapter();
            updateChapter.setId(request.getId());

            if (request.getTitle() != null) {
                updateChapter.setTitle(request.getTitle());
            }
            if (request.getContent() != null) {
                updateChapter.setContent(request.getContent());
                updateChapter.setWordCount(request.getContent().length());
            }
            if (request.getStatus() != null) {
                updateChapter.setStatus(request.getStatus());
            }

            chapterMapper.updateById(updateChapter);
            log.info("更新章节: chapterId={}", request.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long chapterId) {
        NovelChapter chapter = getChapterWithPermissionCheck(userId, chapterId);

        // 删除章节
        chapterMapper.deleteById(chapterId);

        // 删除关联的记忆
        storyMemoryService.deleteByChapter(userId, chapter.getProjectId(), chapterId);

        log.info("删除章节: userId={}, chapterId={}", userId, chapterId);
    }

    @Override
    public ChapterContextVO getGenerationContext(Long userId, Long projectId, Long outlineId) {
        // 检查项目权限
        NovelProject project = projectService.checkOwnership(userId, projectId);

        // 获取大纲实体（权限已校验）
        OutlineVO outline = outlineService.getByIdInternal(outlineId);

        // 计算当前应该的章节号
        Integer chapterNumber = calculateChapterNumber(projectId, outline, 0);

        // 构建上下文
        ChapterGenerateRequest dummyRequest = new ChapterGenerateRequest();
        dummyRequest.setEnableMemoryRetrieval(true);

        return contextBuilder.buildContext(userId, project, outline, chapterNumber, dummyRequest);
    }

    /**
     * 获取章节并检查权限，同时返回项目实体
     */
    private ChapterWithProject getChapterWithProject(Long userId, Long chapterId) {
        NovelChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "章节不存在");
        }

        // 检查项目权限并返回项目实体
        NovelProject project = projectService.checkOwnership(userId, chapter.getProjectId());

        return new ChapterWithProject(chapter, project);
    }

    /**
     * 获取章节并检查权限（仅需章节时使用）
     */
    private NovelChapter getChapterWithPermissionCheck(Long userId, Long chapterId) {
        return getChapterWithProject(userId, chapterId).chapter();
    }

    /**
     * 章节与项目的组合记录
     */
    private record ChapterWithProject(NovelChapter chapter, NovelProject project) {}

    /**
     * 转换为VO
     */
    private ChapterVO convertToVO(NovelChapter chapter) {
        ChapterVO vo = new ChapterVO();
        BeanUtils.copyProperties(chapter, vo);
        return vo;
    }

    @Override
    public Flux<String> denoiseChapter(Long userId, Long chapterId) {
        // 检查章节权限
        NovelChapter chapter = getChapterWithPermissionCheck(userId, chapterId);

        // 检查章节是否有内容
        Assert.isTrue(chapter.getContent() != null && !chapter.getContent().isBlank(),
                "章节内容为空，无法去味");

        log.info("开始AI去味: userId={}, chapterId={}, wordCount={}",
                userId, chapterId, chapter.getWordCount());

        // 使用 PromptTemplates.AI_DENOISING 模板构建提示词
        String prompt = PromptTemplates.AI_DENOISING
                .replace("{original_text}", chapter.getContent());

        // 创建ChatClient并流式生成
        ChatClient chatClient = chatClientFactory.createForUser(userId);
        AtomicReference<String> fullContent = new AtomicReference<>("");

        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                .doOnNext(chunk -> fullContent.updateAndGet(current -> current + chunk))
                .doOnComplete(() -> {
                    // 更新章节内容
                    String denoisedContent = fullContent.get();
                    NovelChapter updateChapter = new NovelChapter();
                    updateChapter.setId(chapter.getId());
                    updateChapter.setContent(denoisedContent);
                    updateChapter.setWordCount(denoisedContent.length());
                    chapterMapper.updateById(updateChapter);

                    // 异步生成新摘要
                    chapterAsyncService.asyncGenerateSummary(chapter.getId(), denoisedContent);

                    log.info("AI去味完成: chapterId={}, originalWordCount={}, newWordCount={}",
                            chapter.getId(), chapter.getWordCount(), denoisedContent.length());
                })
                .doOnError(error -> {
                    log.error("AI去味失败: chapterId={}", chapter.getId(), error);
                });
    }
}
