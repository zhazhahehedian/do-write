package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 章节润色请求
 *
 * @author dpbug
 */
@Data
public class ChapterPolishRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 章节ID
     */
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    /**
     * 润色类型
     * <ul>
     *     <li>enhance_description - 增强描写</li>
     *     <li>fix_grammar - 修正语法</li>
     *     <li>adjust_pacing - 调整节奏</li>
     *     <li>all - 全面润色</li>
     * </ul>
     */
    private String polishType;

    /**
     * 润色使用的风格ID
     */
    private Long styleId;

    /**
     * 自定义润色要求
     */
    private String customInstructions;
}
