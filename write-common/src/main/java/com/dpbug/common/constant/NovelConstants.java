package com.dpbug.common.constant;

/**
 * 小说常量
 *
 * @author dpbug
 */
public interface NovelConstants {

    // ==================== 项目相关 ====================

    /**
     * 项目状态
     */
    interface ProjectStatus {
        /**
         * 规划中
         */
        String PLANNING = "planning";
        /**
         * 创作中
         */
        String WRITING = "writing";
        /**
         * 已完成
         */
        String COMPLETED = "completed";
    }

    /**
     * 向导状态
     */
    interface WizardStatus {
        /**
         * 未开始
         */
        String NOT_STARTED = "not_started";
        /**
         * 进行中
         */
        String IN_PROGRESS = "in_progress";
        /**
         * 已完成
         */
        String COMPLETED = "completed";
    }

    /**
     * 向导步骤
     */
    interface WizardStep {
        /**
         * 初始（基本信息）
         */
        int INIT = 0;
        /**
         * 世界观生成
         */
        int WORLD = 1;
        /**
         * 角色生成
         */
        int CHARACTERS = 2;
        /**
         * 大纲生成
         */
        int OUTLINES = 3;
    }

    /**
     * 大纲模式
     */
    interface OutlineMode {
        /**
         * 一对一
         */
        String ONE_TO_ONE = "one-to-one";
        /**
         * 一对多
         */
        String ONE_TO_MANY = "one-to-many";
    }

    /**
     * 叙事视角
     */
    interface Perspective {
        /**
         * 第一人称
         */
        String FIRST_PERSON = "first_person";
        /**
         * 第三人称
         */
        String THIRD_PERSON = "third_person";
    }

    /**
     * 角色类型
     */
    interface RoleType {
        /**
         * 主角
         */
        String PROTAGONIST = "protagonist";
        /**
         * 反派
         */
        String ANTAGONIST = "antagonist";
        /**
         * 配角
         */
        String SUPPORTING = "supporting";
    }

    /**
     * 小说类型
     */
    interface Genre {
        String FANTASY = "玄幻";
        String URBAN = "都市";
        String SCIFI = "科幻";
        String ROMANCE = "言情";
        String HISTORY = "历史";
        String MYSTERY = "悬疑";
        String WUXIA = "武侠";
        String GAME = "游戏";
    }

    // ==================== 章节相关 ====================

    /**
     * 章节状态
     */
    interface ChapterStatus {
        /**
         * 草稿
         */
        String DRAFT = "draft";
        /**
         * 已发布
         */
        String PUBLISHED = "published";
        /**
         * 已归档
         */
        String ARCHIVED = "archived";
    }

    /**
     * 生成状态
     */
    interface GenerationStatus {
        /**
         * 待生成
         */
        String PENDING = "pending";
        /**
         * 生成中
         */
        String GENERATING = "generating";
        /**
         * 已完成
         */
        String COMPLETED = "completed";
        /**
         * 失败
         */
        String FAILED = "failed";
    }

    // ==================== 记忆相关 ====================

    /**
     * 记忆类型
     */
    interface MemoryType {
        /**
         * 情节点
         */
        String PLOT_POINT = "plot_point";
        /**
         * 钩子（悬念）
         */
        String HOOK = "hook";
        /**
         * 伏笔
         */
        String FORESHADOW = "foreshadow";
        /**
         * 角色事件
         */
        String CHARACTER_EVENT = "character_event";
        /**
         * 地点事件
         */
        String LOCATION_EVENT = "location_event";
    }

    /**
     * 伏笔状态
     */
    interface ForeshadowStatus {
        /**
         * 普通记忆
         */
        int NORMAL = 0;
        /**
         * 已埋下的伏笔
         */
        int PLANTED = 1;
        /**
         * 已回收的伏笔
         */
        int RESOLVED = 2;
    }

    // ==================== 任务相关 ====================

    /**
     * 任务类型
     */
    interface TaskType {
        /**
         * 单章生成
         */
        String SINGLE_CHAPTER = "single_chapter";
        /**
         * 批量生成
         */
        String BATCH_CHAPTER = "batch_chapter";
        /**
         * 剧情分析
         */
        String ANALYSIS = "analysis";
    }

    /**
     * 任务状态
     */
    interface TaskStatus {
        /**
         * 待执行
         */
        String PENDING = "pending";
        /**
         * 执行中
         */
        String RUNNING = "running";
        /**
         * 已完成
         */
        String COMPLETED = "completed";
        /**
         * 失败
         */
        String FAILED = "failed";
        /**
         * 已取消
         */
        String CANCELLED = "cancelled";
    }

    // ==================== 润色相关 ====================

    /**
     * 润色类型
     */
    interface PolishType {
        /**
         * 增强描写
         */
        String ENHANCE_DESCRIPTION = "enhance_description";
        /**
         * 修正语法
         */
        String FIX_GRAMMAR = "fix_grammar";
        /**
         * 调整节奏
         */
        String ADJUST_PACING = "adjust_pacing";
        /**
         * 全面润色
         */
        String ALL = "all";
    }

    // ==================== 配置常量 ====================

    /**
     * 章节配置常量
     */
    interface ChapterConfig {
        /**
         * 默认目标字数
         */
        int DEFAULT_WORD_COUNT = 3000;
        /**
         * 最小字数
         */
        int MIN_WORD_COUNT = 500;
        /**
         * 最大字数
         */
        int MAX_WORD_COUNT = 10000;
        /**
         * 上下文最近章节数
         */
        int RECENT_CHAPTERS_FOR_CONTEXT = 3;
        /**
         * 骨架采样间隔（每N章采样1章）
         */
        int SKELETON_SAMPLE_INTERVAL = 50;
        /**
         * RAG检索数量
         */
        int MEMORY_TOP_K = 15;
        /**
         * 相似度阈值
         */
        double SIMILARITY_THRESHOLD = 0.3;
        /**
         * 摘要最大长度
         */
        int MAX_SUMMARY_LENGTH = 200;
    }

}
