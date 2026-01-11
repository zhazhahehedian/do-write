package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 剧情分析请求
 */
@Data
public class PlotAnalysisRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 章节ID
     */
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    /**
     * 是否强制重新分析（默认 false：如果已存在分析结果则直接返回）
     */
    private Boolean force;
}

