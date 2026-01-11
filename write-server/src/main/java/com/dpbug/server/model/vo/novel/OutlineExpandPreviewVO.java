package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 大纲展开预览结果
 *
 * @author dpbug
 */
@Data
public class OutlineExpandPreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 大纲ID
     */
    private Long outlineId;

    /**
     * 大纲标题
     */
    private String outlineTitle;

    /**
     * 大纲内容
     */
    private String outlineContent;

    /**
     * 章节规划列表
     */
    private List<ChapterPlanVO> chapterPlans;

    /**
     * 章节规划VO
     */
    @Data
    public static class ChapterPlanVO implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 子章节序号（从1开始）
         */
        private Integer subIndex;

        /**
         * 章节标题
         */
        private String title;

        /**
         * 剧情摘要
         */
        private String plotSummary;

        /**
         * 关键事件列表
         */
        private List<String> keyEvents;

        /**
         * 角色焦点
         */
        private List<String> characterFocus;

        /**
         * 情绪基调
         */
        private String emotionalTone;

        /**
         * 叙事目标
         */
        private String narrativeGoal;

        /**
         * 冲突类型
         */
        private String conflictType;

        /**
         * 预估字数
         */
        private Integer estimatedWords;

        /**
         * 场景列表
         */
        private List<SceneVO> scenes;
    }

    /**
     * 场景VO
     */
    @Data
    public static class SceneVO implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 场景地点
         */
        private String location;

        /**
         * 参与角色
         */
        private List<String> characters;

        /**
         * 场景目的
         */
        private String purpose;
    }
}
