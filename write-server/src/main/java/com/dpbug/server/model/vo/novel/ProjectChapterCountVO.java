package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.io.Serializable;

/**
 * 项目章节数统计VO
 */
@Data
public class ProjectChapterCountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 已生成章节数（generation_status = completed）
     */
    private Integer chapterCount;
}
