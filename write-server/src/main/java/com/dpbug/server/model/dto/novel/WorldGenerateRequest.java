package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 世界观生成请求
 */
@Data
public class WorldGenerateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    // 可选：用户自定义世界观要求
    private String customRequirements;
}
