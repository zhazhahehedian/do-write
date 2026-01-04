package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目详情响应VO
 *
 * @author dpbug
 */
@Data
public class ProjectVO implements Serializable {

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
     * 简介
     */
    private String description;

    /**
     * 主题
     */
    private String theme;

    /**
     * 类型
     */
    private String genre;

    /**
     * 目标字数
     */
    private Integer targetWords;

    /**
     * 当前字数
     */
    private Integer currentWords;

    /**
     * 状态（planning/writing/completed）
     */
    private String status;

    /**
     * 世界观 - 时间背景
     */
    private String worldTimePeriod;

    /**
     * 世界观 - 地理位置
     */
    private String worldLocation;

    /**
     * 世界观 - 氛围基调
     */
    private String worldAtmosphere;

    /**
     * 世界观 - 世界规则
     */
    private String worldRules;

    /**
     * 计划章节数
     */
    private Integer chapterCount;

    /**
     * 叙事视角
     */
    private String narrativePerspective;

    /**
     * 角色数量
     */
    private Integer characterCount;

    /**
     * 大纲模式（one-to-one/one-to-many）
     */
    private String outlineMode;

    /**
     * 向导状态
     */
    private String wizardStatus;

    /**
     * 向导步骤
     */
    private Integer wizardStep;

    /**
     * AI模型名称
     */
    private String aiModel;

    /**
     * 写作风格编码
     */
    private String writingStyleCode;

    /**
     * 写作风格名称（关联查询）
     */
    private String writingStyleName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 实际章节数（统计字段，可选）
     */
    private Integer actualChapterCount;

    /**
     * 实际角色数（统计字段，可选）
     */
    private Integer actualCharacterCount;

    /**
     * 实际大纲数（统计字段，可选）
     */
    private Integer actualOutlineCount;
}