package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 章节列表响应
 *
 * @author dpbug
 */
@Data
public class ChapterVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 章节ID
     */
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 大纲ID
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
     * 字数统计
     */
    private Integer wordCount;

    /**
     * 内容状态: draft/published/archived
     */
    private String status;

    /**
     * 生成状态: pending/generating/completed/failed
     */
    private String generationStatus;

    /**
     * 使用的AI模型
     */
    private String aiModel;

    /**
     * 写作风格编码
     */
    private String styleCode;

    /**
     * 写作风格名称
     */
    private String styleName;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
