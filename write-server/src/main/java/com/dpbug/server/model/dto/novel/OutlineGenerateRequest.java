package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 大纲生成请求
 */
@Data
public class OutlineGenerateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @Min(value = 1, message = "大纲数量至少为1")
    private Integer outlineCount;  // 大纲数量（从项目配置读取）

    // 可选：用户自定义大纲要求
    private String customRequirements;
}
