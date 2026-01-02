package com.dpbug.server.model.entity.novel;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.dpbug.server.model.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 小说章节实体类
 *
 * @author dpbug
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "novel_chapter", autoResultMap = true)
public class NovelChapter extends BaseEntity {

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 关联大纲ID
     */
    private Long outlineId;

    /**
     * 章节序号
     */
    private Integer chapterNumber;

    /**
     * one-to-many模式下的子序号
     */
    private Integer subIndex;

    /**
     * 章节标题
     */
    private String title;

    /**
     * 章节内容
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String content;

    /**
     * 章节摘要（自动生成）
     */
    private String summary;

    /**
     * 字数统计
     */
    private Integer wordCount;

    /**
     * 状态: draft/published/archived
     */
    private String status;

    /**
     * 生成状态: pending/generating/completed/failed
     */
    private String generationStatus;

    /**
     * one-to-many展开规划
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> expansionPlan;

    /**
     * 使用的AI模型
     */
    private String aiModel;

    /**
     * 生成参数（温度、top_p等）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> generationParams;

    /**
     * 写作风格ID
     */
    private Long styleId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 上一版本ID
     */
    private Long previousVersionId;
}
