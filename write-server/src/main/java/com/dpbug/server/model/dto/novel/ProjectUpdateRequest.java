package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 项目更新请求DTO
 *
 * @author dpbug
 */
@Data
public class ProjectUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    @NotNull(message = "项目ID不能为空")
    private Long id;

    /**
     * 书名
     */
    @Size(max = 200, message = "书名长度不能超过200个字符")
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
     * 大纲模式
     */
    private String outlineMode;

    /**
     * AI模型名称
     */
    private String aiModel;

    /**
     * 写作风格ID
     */
    private Long writingStyleId;
}