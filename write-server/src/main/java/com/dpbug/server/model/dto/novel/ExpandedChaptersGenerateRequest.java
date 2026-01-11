package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 一纲多章：批量生成已展开子章节请求
 *
 * @author dpbug
 */
@Data
public class ExpandedChaptersGenerateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 写作风格编码（为空则沿用章节记录中的风格）
     */
    private String styleCode;

    /**
     * 目标字数
     */
    @Min(value = 500, message = "目标字数不能少于500")
    @Max(value = 10000, message = "目标字数不能超过10000")
    private Integer targetWordCount;

    /**
     * 临时指定叙事人称（第一人称/第三人称）
     */
    private String narrativePerspective;

    /**
     * 用户自定义要求（会追加到每个子章节生成提示中）
     */
    private String customRequirements;

    /**
     * 温度，默认沿用单章逻辑（为空则使用默认值）
     */
    @Min(value = 0, message = "温度不能小于0")
    @Max(value = 2, message = "温度不能大于2")
    private Double temperature;

    /**
     * top_p，默认沿用单章逻辑（为空则使用默认值）
     */
    @Min(value = 0, message = "top_p不能小于0")
    @Max(value = 1, message = "top_p不能大于1")
    private Double topP;

    /**
     * 是否启用记忆检索（为空则使用默认值）
     */
    private Boolean enableMemoryRetrieval;
}

