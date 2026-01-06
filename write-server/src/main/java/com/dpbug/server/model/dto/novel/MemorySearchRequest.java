package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @author dpbug
 * @description
 */
@Data
public class MemorySearchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID（必填）
     */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /**
     * 搜索查询文本（必填）
     */
    @NotBlank(message = "查询文本不能为空")
    private String query;

    /**
     * 返回数量（默认5）
     */
    private Integer topK = 5;

    /**
     * 相似度阈值（默认0.3）
     */
    private Double threshold = 0.3;
}
