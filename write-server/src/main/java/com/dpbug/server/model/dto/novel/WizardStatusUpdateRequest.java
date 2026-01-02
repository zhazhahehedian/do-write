package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 向导状态更新请求
 */
@Data
public class WizardStatusUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @NotBlank(message = "向导状态不能为空")
    private String status;  // not_started/in_progress/completed

    @NotNull(message = "向导步骤不能为空")
    private Integer step;   // 0-3
}
