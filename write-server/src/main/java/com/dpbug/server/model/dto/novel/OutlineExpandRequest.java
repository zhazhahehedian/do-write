package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 大纲展开请求
 *
 * @author dpbug
 */
@Data
public class OutlineExpandRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 大纲ID
     */
    @NotNull(message = "大纲ID不能为空")
    private Long outlineId;

    /**
     * 目标子章节数量 (2-10)
     */
    @Min(value = 2, message = "至少展开为2个子章节")
    @Max(value = 10, message = "最多展开为10个子章节")
    private Integer targetChapterCount = 3;

    /**
     * 展开策略
     * - balanced: 均衡分布
     * - climax: 高潮集中
     * - detail: 细节展开
     */
    private String strategy = "balanced";

    /**
     * 是否启用场景分析
     */
    private Boolean enableSceneAnalysis = false;

    /**
     * 自定义要求
     */
    private String customRequirements;
}
