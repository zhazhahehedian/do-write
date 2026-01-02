package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.io.Serializable;

/**
 * 章节摘要响应
 * <p>
 * 用于上下文构建，只包含必要字段，减少数据传输
 *
 * @author dpbug
 */
@Data
public class ChapterSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 章节ID
     */
    private Long id;

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
     * 章节摘要
     */
    private String summary;

    /**
     * 字数统计
     */
    private Integer wordCount;
}
