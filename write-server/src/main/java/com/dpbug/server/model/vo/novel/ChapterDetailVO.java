package com.dpbug.server.model.vo.novel;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 章节详情响应
 * <p>
 * 继承 ChapterVO，包含完整内容和关联信息
 *
 * @author dpbug
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChapterDetailVO extends ChapterVO {

    private static final long serialVersionUID = 1L;

    /**
     * 章节完整内容
     */
    private String content;

    /**
     * 章节摘要
     */
    private String summary;

    /**
     * 展开规划（one-to-many模式）
     */
    private Map<String, Object> expansionPlan;

    /**
     * 生成参数
     */
    private Map<String, Object> generationParams;

    // ========== 关联信息 ==========

    /**
     * 大纲标题
     */
    private String outlineTitle;

    /**
     * 大纲内容
     */
    private String outlineContent;

    /**
     * 上一章
     */
    private ChapterVO previousChapter;

    /**
     * 下一章
     */
    private ChapterVO nextChapter;
}
