package com.dpbug.server.model.dto.novel;

import com.dpbug.common.domain.PageRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 章节查询请求
 *
 * @author dpbug
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChapterQueryRequest extends PageRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /**
     * 按内容状态过滤: draft/published/archived
     */
    private String status;

    /**
     * 按生成状态过滤: pending/generating/completed/failed
     */
    private String generationStatus;

    /**
     * 按大纲ID过滤
     */
    private Long outlineId;

    /**
     * 关键词搜索（标题/内容）
     */
    private String keyword;
}
