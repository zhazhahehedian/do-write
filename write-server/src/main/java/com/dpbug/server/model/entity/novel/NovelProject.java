package com.dpbug.server.model.entity.novel;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dpbug.server.model.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 *
 * @author dpbug
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_project")
public class NovelProject extends BaseEntity {

    /**
     * 用户id
     */
    private Long userId;

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
     * 状态
     */
    private String status;

    /**
     * 世界观-时间背景
     */
    private String worldTimePeriod;

    /**
     * 世界观-地理位置
     */
    private String worldLocation;

    /**
     * 世界观-氛围基调
     */
    private String worldAtmosphere;

    /**
     * 世界观-世界规则
     */
    private String worldRules;

    /**
     * 计划章节数
     */
    private Integer chapterCount;

    /**
     * 叙事视角(第一人称/第三人称)
     */
    private String narrativePerspective;

    /**
     * 角色数量
     */
    private Integer characterCount;

    /**
     * 大纲模式 (one-to-one/one-to-many)
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
     * AI模型
     */
    private String aiModel;

    /**
     * 写作风格id
     */
    private Long writingStyleId;
}
