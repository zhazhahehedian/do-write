package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 章节更新请求
 *
 * @author dpbug
 */
@Data
public class ChapterUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 章节ID
     */
    @NotNull(message = "章节ID不能为空")
    private Long id;

    /**
     * 章节标题
     */
    private String title;

    /**
     * 章节内容
     */
    private String content;

    /**
     * 状态: draft/published/archived
     */
    private String status;

    /**
     * 是否创建新版本，默认false
     * <p>
     * 设为true时，会保留当前内容创建新版本，而不是直接覆盖
     */
    private Boolean createNewVersion;
}
