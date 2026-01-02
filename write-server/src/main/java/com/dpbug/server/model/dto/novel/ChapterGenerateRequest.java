package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 单章生成请求
 *
 * @author dpbug
 */
@Data
public class ChapterGenerateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /**
     * 大纲ID
     */
    @NotNull(message = "大纲ID不能为空")
    private Long outlineId;

    /**
     * one-to-many模式下的子序号，默认0
     */
    private Integer subIndex;

    /**
     * 写作风格ID，为空则使用项目默认
     */
    private Long styleId;

    /**
     * 目标字数，默认从项目配置读取
     */
    @Min(value = 500, message = "目标字数不能少于500")
    @Max(value = 10000, message = "目标字数不能超过10000")
    private Integer targetWordCount;

    /**
     * 临时指定叙事人称（第一人称/第三人称）
     */
    private String narrativePerspective;

    /**
     * 用户自定义要求
     */
    private String customRequirements;

    // ========== 高级参数 ==========

    /**
     * 温度，默认0.7
     */
    @Min(value = 0, message = "温度不能小于0")
    @Max(value = 2, message = "温度不能大于2")
    private Double temperature;

    /**
     * top_p，默认0.9
     */
    @Min(value = 0, message = "top_p不能小于0")
    @Max(value = 1, message = "top_p不能大于1")
    private Double topP;

    /**
     * 是否启用记忆检索，默认true
     */
    private Boolean enableMemoryRetrieval;
}
