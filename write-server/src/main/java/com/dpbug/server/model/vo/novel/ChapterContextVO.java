package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 章节生成上下文响应
 * <p>
 * 用于展示/调试生成时使用的完整上下文信息
 *
 * @author dpbug
 */
@Data
public class ChapterContextVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 项目信息 ==========

    /**
     * 项目标题
     */
    private String projectTitle;

    /**
     * 类型（玄幻/都市/科幻等）
     */
    private String genre;

    /**
     * 主题
     */
    private String theme;

    // ========== 世界观 ==========

    /**
     * 时间背景
     */
    private String worldTimePeriod;

    /**
     * 地理位置
     */
    private String worldLocation;

    /**
     * 氛围基调
     */
    private String worldAtmosphere;

    /**
     * 世界规则
     */
    private String worldRules;

    // ========== 角色信息 ==========

    /**
     * 主要角色列表
     */
    private List<CharacterVO> mainCharacters;

    // ========== 大纲信息 ==========

    /**
     * 当前大纲
     */
    private OutlineVO currentOutline;

    // ========== 历史章节 ==========

    /**
     * 最近章节摘要（用于直接衔接）
     */
    private List<ChapterSummaryVO> recentChapters;

    /**
     * 故事骨架（每N章采样1章，用于长篇小说）
     */
    private List<ChapterSummaryVO> skeletonChapters;

    // ========== RAG记忆 ==========

    /**
     * 语义相关记忆
     */
    private List<StoryMemoryVO> relatedMemories;

    /**
     * 未回收伏笔
     */
    private List<StoryMemoryVO> pendingForeshadows;

    /**
     * 角色状态记忆
     */
    private List<StoryMemoryVO> characterStates;

    // ========== 写作风格 ==========

    /**
     * 写作风格提示词
     */
    private String writingStylePrompt;
}
