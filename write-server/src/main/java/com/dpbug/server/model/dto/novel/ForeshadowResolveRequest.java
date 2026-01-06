package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @author dpbug
 * @description
 */
@Data
public class ForeshadowResolveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记忆ID（必填）
     */
    @NotNull(message = "记忆ID不能为空")
    private Long memoryId;

    /**
     * 回收章节ID（必填）
     */
    @NotNull(message = "回收章节ID不能为空")
    private Long resolvedAtChapterId;
}
