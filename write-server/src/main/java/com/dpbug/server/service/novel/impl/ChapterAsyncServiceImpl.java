package com.dpbug.server.service.novel.impl;

import com.dpbug.common.constant.NovelConstants;
import com.dpbug.server.ai.ChatClientFactory;
import com.dpbug.server.ai.prompt.PromptTemplates;
import com.dpbug.server.ai.prompt.WritingStyleManager;
import com.dpbug.server.ai.prompt.model.WritingStyle;
import com.dpbug.server.mapper.novel.ChapterMapper;
import com.dpbug.server.model.dto.novel.BatchGenerateRequest;
import com.dpbug.server.model.dto.novel.ChapterGenerateRequest;
import com.dpbug.server.model.entity.novel.NovelChapter;
import com.dpbug.server.model.entity.novel.NovelGenerationTask;
import com.dpbug.server.model.entity.novel.NovelProject;
import com.dpbug.server.model.entity.novel.NovelStoryMemory;
import com.dpbug.server.model.vo.novel.ChapterContextVO;
import com.dpbug.server.model.vo.novel.ChapterSummaryVO;
import com.dpbug.server.model.vo.novel.CharacterVO;
import com.dpbug.server.model.vo.novel.OutlineVO;
import com.dpbug.server.model.vo.novel.StoryMemoryVO;
import com.dpbug.server.service.novel.ChapterAsyncService;
import com.dpbug.server.service.novel.ChapterContextBuilder;
import com.dpbug.server.service.novel.GenerationTaskService;
import com.dpbug.server.service.novel.OutlineService;
import com.dpbug.server.service.novel.ProjectService;
import com.dpbug.server.service.novel.StoryMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 章节异步服务实现类
 *
 * <p>将异步方法抽取到单独的类中，确保 Spring AOP 能够正确拦截 @Async 方法</p>
 *
 * @author dpbug
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterAsyncServiceImpl implements ChapterAsyncService {

    private final ChapterMapper chapterMapper;
    private final StoryMemoryService storyMemoryService;
    private final GenerationTaskService taskService;
    private final OutlineService outlineService;
    private final ProjectService projectService;
    private final ChatClientFactory chatClientFactory;
    private final ChapterContextBuilder chapterContextBuilder;
    private final WritingStyleManager writingStyleManager;

    @Override
    @Async("chapterExecutor")
    public void asyncExtractMemories(Long userId, Long projectId, Long chapterId,
                                     Integer chapterNumber, String content) {
        log.info("开始异步提取记忆: userId={}, projectId={}, chapterId={}", userId, projectId, chapterId);
        try {
            // 提取记忆
            List<NovelStoryMemory> memories = storyMemoryService.extractMemories(
                    userId, projectId, chapterId, chapterNumber, content);
            if (!memories.isEmpty()) {
                storyMemoryService.saveMemories(projectId, memories);
                log.info("记忆提取完成: chapterId={}, count={}", chapterId, memories.size());
            }
        } catch (Exception e) {
            log.error("提取记忆失败: chapterId={}", chapterId, e);
        }
    }

    @Override
    @Async("chapterExecutor")
    public void asyncGenerateSummary(Long chapterId, String content) {
        log.info("开始异步生成摘要: chapterId={}", chapterId);
        try {
            // 简单实现：截取前200字作为摘要
            // TODO: 后续可以使用AI生成更精准的摘要
            String summary;
            if (content.length() > NovelConstants.ChapterConfig.MAX_SUMMARY_LENGTH) {
                summary = content.substring(0, NovelConstants.ChapterConfig.MAX_SUMMARY_LENGTH) + "...";
            } else {
                summary = content;
            }

            NovelChapter chapter = new NovelChapter();
            chapter.setId(chapterId);
            chapter.setSummary(summary);
            chapterMapper.updateById(chapter);

            log.info("摘要生成完成: chapterId={}", chapterId);
        } catch (Exception e) {
            log.error("生成摘要失败: chapterId={}", chapterId, e);
        }
    }

    @Override
    @Async("chapterExecutor")
    public void asyncBatchGenerate(Long userId, NovelGenerationTask task, BatchGenerateRequest request) {
        log.info("开始批量生成章节: userId={}, taskId={}, count={}",
                userId, task.getId(), request.getOutlineIds().size());

        List<Long> outlineIds = request.getOutlineIds();
        int total = outlineIds.size();
        int completed = 0;
        int failed = 0;

        // 更新任务状态为运行中
        taskService.updateProgress(task.getId(), 0, "开始批量生成");

        for (int i = 0; i < outlineIds.size(); i++) {
            Long outlineId = outlineIds.get(i);

            try {
                // 更新进度
                int progress = (i * 100) / total;
                taskService.updateProgress(task.getId(), progress,
                        String.format("正在生成第 %d/%d 章", i + 1, total));

                // 同步生成单章
                generateChapterSync(userId, request.getProjectId(), outlineId, request);
                completed++;

                log.info("批量生成进度: taskId={}, {}/{}", task.getId(), completed, total);
            } catch (Exception e) {
                failed++;
                log.error("批量生成单章失败: outlineId={}", outlineId, e);
                // 继续下一章，不中断整个任务
            }
        }

        // 完成任务
        Map<String, Object> result = new HashMap<>();
        result.put("totalChapters", total);
        result.put("completedChapters", completed);
        result.put("failedChapters", failed);

        if (failed == total) {
            taskService.failTask(task.getId(), "所有章节生成失败");
        } else {
            taskService.completeTask(task.getId(), result);
        }

        log.info("批量生成完成: taskId={}, completed={}, failed={}", task.getId(), completed, failed);
    }

    /**
     * 同步生成单个章节（供批量生成调用）
     * 使用完整的 PromptTemplates 模板和上下文构建
     */
    private void generateChapterSync(Long userId, Long projectId, Long outlineId, BatchGenerateRequest batchRequest) {
        // 获取项目信息
        NovelProject project = projectService.checkOwnership(userId, projectId);

        // 获取大纲
        OutlineVO outline = outlineService.getByIdInternal(outlineId);
        if (outline == null) {
            throw new IllegalArgumentException("大纲不存在: " + outlineId);
        }

        // 创建章节记录
        NovelChapter chapter = new NovelChapter();
        chapter.setProjectId(projectId);
        chapter.setOutlineId(outlineId);
        chapter.setChapterNumber(outline.getOrderIndex());
        chapter.setSubIndex(0);
        chapter.setTitle(outline.getTitle());
        chapter.setStatus(NovelConstants.ChapterStatus.DRAFT);
        chapter.setGenerationStatus(NovelConstants.GenerationStatus.GENERATING);
        chapter.setStyleCode(batchRequest.getStyleCode());
        chapter.setVersion(1);

        Integer targetWordCount = batchRequest.getTargetWordCount() != null ?
                batchRequest.getTargetWordCount() : NovelConstants.ChapterConfig.DEFAULT_WORD_COUNT;

        Map<String, Object> params = new HashMap<>();
        params.put("targetWordCount", targetWordCount);
        chapter.setGenerationParams(params);

        chapterMapper.insert(chapter);

        try {
            // 构建生成请求
            ChapterGenerateRequest generateRequest = new ChapterGenerateRequest();
            generateRequest.setProjectId(projectId);
            generateRequest.setOutlineId(outlineId);
            generateRequest.setStyleCode(batchRequest.getStyleCode());
            generateRequest.setTargetWordCount(targetWordCount);
            generateRequest.setEnableMemoryRetrieval(true);

            // 使用 ChapterContextBuilder 构建完整上下文
            ChapterContextVO context = chapterContextBuilder.buildContext(
                    project, outline, outline.getOrderIndex(), generateRequest);

            // 使用 PromptTemplates 构建提示词
            String prompt = buildPromptFromTemplate(project, outline, context, targetWordCount);

            // 应用写作风格
            if (batchRequest.getStyleCode() != null) {
                WritingStyle style = WritingStyle.fromCode(batchRequest.getStyleCode());
                if (style != null) {
                    prompt = writingStyleManager.applyStyleToPrompt(prompt, style);
                }
            }

            // 调用AI生成（同步方式）
            ChatClient chatClient = chatClientFactory.createForUser(userId);
            String content = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 保存内容
            chapter.setContent(content);
            chapter.setWordCount(content != null ? content.length() : 0);
            chapter.setGenerationStatus(NovelConstants.GenerationStatus.COMPLETED);
            chapter.setAiModel("batch-model");
            chapterMapper.updateById(chapter);

            // 异步提取记忆
            if (content != null && !content.isEmpty()) {
                asyncExtractMemories(userId, projectId, chapter.getId(), chapter.getChapterNumber(), content);
            }

        } catch (Exception e) {
            // 标记生成失败
            chapter.setGenerationStatus(NovelConstants.GenerationStatus.FAILED);
            chapterMapper.updateById(chapter);
            throw e;
        }
    }

    /**
     * 使用 PromptTemplates 模板构建提示词
     * <p>根据是否有前置章节选择不同的模板</p>
     */
    private String buildPromptFromTemplate(NovelProject project, OutlineVO outline,
                                           ChapterContextVO context, Integer targetWordCount) {
        // 判断是否有前置章节
        boolean hasContext = context.getRecentChapters() != null && !context.getRecentChapters().isEmpty();

        String template;
        if (hasContext) {
            // 使用带上下文的模板
            template = PromptTemplates.CHAPTER_GENERATION_WITH_CONTEXT;
        } else {
            // 使用基础模板
            template = PromptTemplates.CHAPTER_GENERATION;
        }

        // 计算最大字数（目标字数的120%）
        int maxWordCount = (int) (targetWordCount * 1.2);

        // 替换模板占位符
        String prompt = template
                .replace("{title}", nullToEmpty(project.getTitle()))
                .replace("{theme}", nullToEmpty(project.getTheme()))
                .replace("{genre}", nullToEmpty(project.getGenre()))
                .replace("{narrative_perspective}", nullToEmpty(project.getNarrativePerspective()))
                .replace("{time_period}", nullToEmpty(context.getWorldTimePeriod()))
                .replace("{location}", nullToEmpty(context.getWorldLocation()))
                .replace("{atmosphere}", nullToEmpty(context.getWorldAtmosphere()))
                .replace("{rules}", nullToEmpty(context.getWorldRules()))
                .replace("{characters_info}", buildCharactersInfo(context.getMainCharacters()))
                .replace("{outlines_context}", buildOutlinesContext(outline))
                .replace("{chapter_number}", String.valueOf(outline.getOrderIndex()))
                .replace("{chapter_title}", nullToEmpty(outline.getTitle()))
                .replace("{chapter_outline}", nullToEmpty(outline.getContent()))
                .replace("{target_word_count}", String.valueOf(targetWordCount))
                .replace("{max_word_count}", String.valueOf(maxWordCount));

        // 如果有上下文，还需要替换额外的占位符
        if (hasContext) {
            prompt = prompt
                    .replace("{previous_content}", buildPreviousContent(context.getRecentChapters()))
                    .replace("{memory_context}", buildMemoryContextString(context));
        }

        return prompt;
    }

    /**
     * 构建角色信息字符串
     */
    private String buildCharactersInfo(List<CharacterVO> characters) {
        if (characters == null || characters.isEmpty()) {
            return "暂无角色信息";
        }

        StringBuilder sb = new StringBuilder();
        for (CharacterVO c : characters) {
            sb.append("- ").append(c.getName());
            if (c.getRoleType() != null) {
                sb.append("（").append(c.getRoleType()).append("）");
            }
            sb.append("：").append(nullToEmpty(c.getPersonality())).append("\n");
        }
        return sb.toString();
    }

    /**
     * 构建大纲上下文
     */
    private String buildOutlinesContext(OutlineVO outline) {
        StringBuilder sb = new StringBuilder();
        sb.append("第").append(outline.getOrderIndex()).append("章 ");
        sb.append(outline.getTitle()).append("：");
        sb.append(nullToEmpty(outline.getContent()));
        return sb.toString();
    }

    /**
     * 构建前置章节内容
     */
    private String buildPreviousContent(List<ChapterSummaryVO> recentChapters) {
        if (recentChapters == null || recentChapters.isEmpty()) {
            return "无前置章节";
        }

        StringBuilder sb = new StringBuilder();
        for (ChapterSummaryVO c : recentChapters) {
            sb.append("【第").append(c.getChapterNumber()).append("章 ");
            sb.append(c.getTitle()).append("】\n");
            sb.append(nullToEmpty(c.getSummary())).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 构建记忆上下文字符串
     */
    private String buildMemoryContextString(ChapterContextVO context) {
        StringBuilder sb = new StringBuilder();

        // 相关记忆
        if (context.getRelatedMemories() != null && !context.getRelatedMemories().isEmpty()) {
            sb.append("【语义相关记忆】\n");
            for (StoryMemoryVO m : context.getRelatedMemories()) {
                sb.append("- [").append(m.getMemoryType()).append("] ");
                sb.append(m.getTitle()).append("：").append(m.getContent()).append("\n");
            }
            sb.append("\n");
        }

        // 未完结伏笔
        if (context.getPendingForeshadows() != null && !context.getPendingForeshadows().isEmpty()) {
            sb.append("【未完结伏笔】\n");
            for (StoryMemoryVO f : context.getPendingForeshadows()) {
                sb.append("- ").append(f.getTitle());
                sb.append("（第").append(f.getStoryTimeline()).append("章埋下）\n");
            }
            sb.append("\n");
        }

        if (sb.isEmpty()) {
            return "暂无相关记忆";
        }

        return sb.toString();
    }

    private String nullToEmpty(String str) {
        return str != null ? str : "";
    }
}
