package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目列表项响应VO（轻量级）
 *
 * @author dpbug
 */
@Data
public class ProjectListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    private Long id;

    /**
     * 书名
     */
    private String title;

    /**
     * 类型
     */
    private String genre;

    /**
     * 主题
     */
    private String theme;

    /**
     * 叙事视角
     */
    private String narrativePerspective;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态（planning/writing/completed）
     */
    private String status;

    /**
     * 目标字数
     */
    private Integer targetWords;

    /**
     * 当前字数
     */
    private Integer currentWords;

    /**
     * 计划章节数（项目配置）
     */
    private Integer chapterCount;

    /**
     * 角色数量（统计字段，通常为实际数量）
     */
    private Integer characterCount;

    /**
     * 实际章节数（统计字段）
     */
    private Integer actualChapterCount;

    /**
     * 向导状态
     */
    private String wizardStatus;

    /**
     * 向导步骤
     */
    private Integer wizardStep;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 进度百分比（计算字段）
     */
    private Integer progressPercent;
}
