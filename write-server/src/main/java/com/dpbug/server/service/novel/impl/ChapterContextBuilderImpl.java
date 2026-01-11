package com.dpbug.server.service.novel.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dpbug.common.constant.NovelConstants;
import com.dpbug.server.ai.prompt.WritingStyleManager;
import com.dpbug.server.mapper.novel.ChapterMapper;
import com.dpbug.server.model.dto.novel.ChapterGenerateRequest;
import com.dpbug.server.model.entity.novel.NovelChapter;
import com.dpbug.server.model.entity.novel.NovelProject;
import com.dpbug.server.model.vo.novel.ChapterContextVO;
import com.dpbug.server.model.vo.novel.ChapterSummaryVO;
import com.dpbug.server.model.vo.novel.CharacterVO;
import com.dpbug.server.model.vo.novel.OutlineVO;
import com.dpbug.server.model.vo.novel.StoryMemoryVO;
import com.dpbug.server.service.novel.ChapterContextBuilder;
import com.dpbug.server.service.novel.CharacterService;
import com.dpbug.server.service.novel.StoryMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 章节上下文构建器实现类
 *
 * @author dpbug
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterContextBuilderImpl implements ChapterContextBuilder {

    private final ChapterMapper chapterMapper;
    private final CharacterService characterService;
    private final StoryMemoryService storyMemoryService;
    private final WritingStyleManager writingStyleManager;

    @Override
    public ChapterContextVO buildContext(Long userId, NovelProject project, OutlineVO outline,
                                          Integer chapterNumber, ChapterGenerateRequest request) {
        ChapterContextVO context = new ChapterContextVO();

        // 1. 项目基本信息
        context.setProjectTitle(project.getTitle());
        context.setGenre(project.getGenre());
        context.setTheme(project.getTheme());

        // 2. 世界观
        context.setWorldTimePeriod(project.getWorldTimePeriod());
        context.setWorldLocation(project.getWorldLocation());
        context.setWorldAtmosphere(project.getWorldAtmosphere());
        context.setWorldRules(project.getWorldRules());

        // 3. 主要角色(主角+重要配角) - 使用内部方法，权限已在外层校验
        List<CharacterVO> characters = characterService.listByProjectInternal(project.getId());
        context.setMainCharacters(filterMainCharacters(characters));

        // 4. 当前大纲
        OutlineVO outlineVO = new OutlineVO();
        BeanUtils.copyProperties(outline, outlineVO);
        context.setCurrentOutline(outlineVO);

        // 5. 历史章节上下文
        buildHistoryContext(context, project.getId(), chapterNumber);

        // 6. RAG记忆上下文
        if (request.getEnableMemoryRetrieval() == null || request.getEnableMemoryRetrieval()) {
            buildMemoryContext(context, userId, project.getId(), outline.getContent());
        }

        // 7. 写作风格
        String styleCode = request.getStyleCode();
        if (styleCode != null && !styleCode.isBlank()) {
            // 优先使用请求中指定的风格
            writingStyleManager.getPresetStyle(styleCode)
                    .ifPresent(style -> context.setWritingStylePrompt(style.getPromptContent()));
        }
        // 如果请求没有指定风格，暂不使用项目默认风格（因为类型不匹配，后续可扩展）

        // 8. 展开规划（one-to-many模式）
        buildExpansionPlanContext(context, outline.getId(), request.getSubIndex());

        return context;
    }

    /**
     * 构建展开规划上下文
     * <p>
     * 查找已存在的章节记录，获取其 expansionPlan
     */
    private void buildExpansionPlanContext(ChapterContextVO context, Long outlineId, Integer subIndex) {
        if (outlineId == null) {
            return;
        }

        // 查询该大纲下的章节，优先按 subIndex 匹配
        LambdaQueryWrapper<NovelChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NovelChapter::getOutlineId, outlineId);
        if (subIndex != null && subIndex > 0) {
            wrapper.eq(NovelChapter::getSubIndex, subIndex);
        } else {
            // 没有指定 subIndex，取第一个
            wrapper.orderByAsc(NovelChapter::getSubIndex);
            wrapper.last("LIMIT 1");
        }

        List<NovelChapter> chapters = chapterMapper.selectList(wrapper);
        if (!chapters.isEmpty()) {
            NovelChapter chapter = chapters.get(0);
            if (chapter.getExpansionPlan() != null && !chapter.getExpansionPlan().isEmpty()) {
                context.setExpansionPlan(chapter.getExpansionPlan());
                log.debug("已加载章节展开规划: outlineId={}, subIndex={}, expansionPlan={}",
                        outlineId, subIndex, chapter.getExpansionPlan());
            }
        }
    }

    /**
     * 过滤主要角色(主角+重要配角,最多5个)
     */
    private List<CharacterVO> filterMainCharacters(List<CharacterVO> characters) {
        if (characters == null || characters.isEmpty()) {
            return Collections.emptyList();
        }

        // 过滤出非组织的角色,并按重要性排序
        return characters.stream()
                .filter(c -> c.getIsOrganization() == null || c.getIsOrganization() == 0)
                .sorted((a, b) -> {
                    // 主角优先,然后反派,最后配角
                    int orderA = getRoleTypeOrder(a.getRoleType());
                    int orderB = getRoleTypeOrder(b.getRoleType());
                    return Integer.compare(orderA, orderB);
                })
                .limit(5)
                .toList();
    }

    /**
     * 角色类型排序权重
     */
    private int getRoleTypeOrder(String roleType) {
        if (NovelConstants.RoleType.PROTAGONIST.equals(roleType)) {
            return 0;
        }
        if (NovelConstants.RoleType.ANTAGONIST.equals(roleType)) {
            return 1;
        }
        return 2; // supporting
    }

    /**
     * 构建历史章节上下文(智能采样策略)
     */
    private void buildHistoryContext(ChapterContextVO context, Long projectId, Integer currentChapterNumber) {
        if (currentChapterNumber == null || currentChapterNumber <= 1) {
            // 第一章,无历史
            context.setRecentChapters(Collections.emptyList());
            context.setSkeletonChapters(Collections.emptyList());
            return;
        }

        // 1. 获取最近3章完整内容(用于直接衔接)
        List<NovelChapter> recentFull = chapterMapper.selectRecentChapters(
                projectId, currentChapterNumber, NovelConstants.ChapterConfig.RECENT_CHAPTERS_FOR_CONTEXT);

        // 转换为摘要VO(保留完整内容用于提示词)
        // 注意: 数据库查询是 DESC 排序，需要反转为正序
        List<ChapterSummaryVO> recentSummaries = new ArrayList<>(recentFull.stream()
                .map(this::toSummaryVO)
                .toList());
        Collections.reverse(recentSummaries);
        context.setRecentChapters(recentSummaries);

        // 2. 如果章节数超过50,构建故事骨架
        if (currentChapterNumber > NovelConstants.ChapterConfig.SKELETON_SAMPLE_INTERVAL) {
            List<NovelChapter> allSummaries = chapterMapper.selectChapterSummaries(
                    projectId, currentChapterNumber);

            List<ChapterSummaryVO> skeleton = new ArrayList<>();
            int interval = NovelConstants.ChapterConfig.SKELETON_SAMPLE_INTERVAL;
            for (int i = 0; i < allSummaries.size(); i += interval) {
                skeleton.add(toSummaryVO(allSummaries.get(i)));
            }
            context.setSkeletonChapters(skeleton);
        } else {
            context.setSkeletonChapters(Collections.emptyList());
        }
    }

    /**
     * 构建RAG记忆上下文
     */
    private void buildMemoryContext(ChapterContextVO context, Long userId, Long projectId, String outlineContent) {
        // 1. 语义检索相关记忆
        List<StoryMemoryVO> relatedMemories = storyMemoryService.searchRelatedMemories(
                userId, projectId, outlineContent, NovelConstants.ChapterConfig.MEMORY_TOP_K);
        context.setRelatedMemories(relatedMemories);

        // 2. 获取未完结伏笔
        List<StoryMemoryVO> foreshadows = storyMemoryService.getPendingForeshadows(projectId);
        context.setPendingForeshadows(foreshadows);

        // 3. 角色状态(从记忆中提取最近的角色事件) - 待实现
        context.setCharacterStates(Collections.emptyList());
    }

    /**
     * 转换为摘要VO
     */
    private ChapterSummaryVO toSummaryVO(NovelChapter chapter) {
        ChapterSummaryVO vo = new ChapterSummaryVO();
        vo.setId(chapter.getId());
        vo.setChapterNumber(chapter.getChapterNumber());
        vo.setTitle(chapter.getTitle());
        vo.setWordCount(chapter.getWordCount());

        // 使用摘要,如果没有则截取内容
        if (chapter.getSummary() != null && !chapter.getSummary().isBlank()) {
            vo.setSummary(chapter.getSummary());
        } else if (chapter.getContent() != null) {
            String content = chapter.getContent();
            int maxLength = NovelConstants.ChapterConfig.MAX_SUMMARY_LENGTH;
            vo.setSummary(content.length() > maxLength
                    ? content.substring(0, maxLength) + "..."
                    : content);
        }

        return vo;
    }

    @Override
    public String buildHistoryContext(Long projectId, Integer currentChapterNumber) {
        // 简化版:返回格式化的历史上下文字符串
        ChapterContextVO context = new ChapterContextVO();
        buildHistoryContext(context, projectId, currentChapterNumber);

        StringBuilder sb = new StringBuilder();
        if (context.getSkeletonChapters() != null && !context.getSkeletonChapters().isEmpty()) {
            sb.append("【故事骨架】\n");
            for (ChapterSummaryVO c : context.getSkeletonChapters()) {
                sb.append("第").append(c.getChapterNumber()).append("章:").append(c.getSummary()).append("\n");
            }
            sb.append("\n");
        }

        if (context.getRecentChapters() != null && !context.getRecentChapters().isEmpty()) {
            sb.append("【最近章节】\n");
            for (ChapterSummaryVO c : context.getRecentChapters()) {
                sb.append("第").append(c.getChapterNumber()).append("章 ").append(c.getTitle());
                sb.append(":").append(c.getSummary()).append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public String buildMemoryContext(Long userId, Long projectId, String outlineContent) {
        List<StoryMemoryVO> memories = storyMemoryService.searchRelatedMemories(
                userId, projectId, outlineContent, NovelConstants.ChapterConfig.MEMORY_TOP_K);

        if (memories.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("【相关情节记忆】\n");
        for (StoryMemoryVO m : memories) {
            sb.append("- [").append(m.getMemoryType()).append("] ");
            sb.append(m.getTitle()).append(":").append(m.getContent()).append("\n");
        }

        return sb.toString();
    }
}