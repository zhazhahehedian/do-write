package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 大纲展开应用请求（创建章节记录）
 *
 * @author dpbug
 */
@Data
public class OutlineExpandApplyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 大纲ID
     */
    @NotNull(message = "大纲ID不能为空")
    private Long outlineId;

    /**
     * 章节规划列表（来自preview结果，可编辑后提交）
     */
    @NotEmpty(message = "章节规划不能为空")
    private List<ChapterPlanDTO> chapterPlans;

    /**
     * 是否强制覆盖（如果该大纲已有章节，是否删除重建）
     */
    private Boolean force = false;

    /**
     * 章节规划DTO
     */
    @Data
    public static class ChapterPlanDTO implements Serializable {
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
         * 场景列表（可选）
         */
        private List<SceneDTO> scenes;
    }

    /**
     * 场景DTO
     */
    @Data
    public static class SceneDTO implements Serializable {
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
