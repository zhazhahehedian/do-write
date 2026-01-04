package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量生成请求
 *
 * @author dpbug
 */
@Data
public class BatchGenerateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /**
     * 要生成的大纲ID列表
     */
    @NotEmpty(message = "大纲ID列表不能为空")
    private List<Long> outlineIds;

    /**
     * 写作风格编码
     */
    private String styleCode;

    /**
     * 目标字数
     */
    @Min(value = 500, message = "目标字数不能少于500")
    @Max(value = 10000, message = "目标字数不能超过10000")
    private Integer targetWordCount;

    /**
     * 是否同步执行剧情分析，默认false
     */
    private Boolean enableAnalysis;

    /**
     * 最大重试次数，默认2
     */
    @Min(value = 0, message = "最大重试次数不能小于0")
    @Max(value = 5, message = "最大重试次数不能大于5")
    private Integer maxRetries;
}
