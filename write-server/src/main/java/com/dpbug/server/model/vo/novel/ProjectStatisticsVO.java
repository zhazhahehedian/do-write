package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.io.Serializable;

/**
 * 项目统计信息响应VO
 *
 * @author dpbug
 */
@Data
public class ProjectStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 书名
     */
    private String title;

    // ==================== 字数统计 ====================

    /**
     * 目标字数
     */
    private Integer targetWords;

    /**
     * 当前字数
     */
    private Integer currentWords;

    /**
     * 进度百分比
     */
    private Integer progressPercent;

    // ==================== 章节统计 ====================

    /**
     * 总章节数
     */
    private Integer totalChapters;

    /**
     * 草稿章节数
     */
    private Integer draftChapters;

    /**
     * 已发布章节数
     */
    private Integer publishedChapters;

    // ==================== 角色统计 ====================

    /**
     * 总角色数
     */
    private Integer totalCharacters;

    /**
     * 主角数量
     */
    private Integer protagonists;

    /**
     * 配角数量
     */
    private Integer supportingRoles;

    /**
     * 反派数量
     */
    private Integer antagonists;

    /**
     * 组织数量
     */
    private Integer organizations;

    // ==================== 大纲统计 ====================

    /**
     * 总大纲数
     */
    private Integer totalOutlines;

    /**
     * 已完成大纲数
     */
    private Integer completedOutlines;

    // ==================== 记忆统计 ====================

    /**
     * 总记忆数
     */
    private Integer totalMemories;

    /**
     * 已埋伏笔数
     */
    private Integer plantedForeshadows;

    /**
     * 已解决伏笔数
     */
    private Integer resolvedForeshadows;
}