package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色生成请求
 */
@Data
public class CharacterGenerateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @Min(value = 1, message = "主角数量至少为1")
    private Integer protagonistCount = 1;  // 主角数量

    @Min(value = 0, message = "配角数量不能为负")
    private Integer supportingCount = 3;   // 配角数量

    @Min(value = 0, message = "反派数量不能为负")
    private Integer antagonistCount = 1;   // 反派数量

    @Min(value = 0, message = "组织数量不能为负")
    private Integer organizationCount = 0; // 组织数量

    // 可选：用户自定义角色要求
    private String customRequirements;
}
