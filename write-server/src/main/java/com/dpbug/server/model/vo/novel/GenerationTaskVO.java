package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 生成任务响应
 *
 * @author dpbug
 */
@Data
public class GenerationTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 任务类型: single_chapter/batch_chapter/analysis
     */
    private String taskType;

    /**
     * 状态: pending/running/completed/failed/cancelled
     */
    private String status;

    /**
     * 进度百分比 0-100
     */
    private Integer progress;

    /**
     * 当前步骤描述
     */
    private String currentStep;

    /**
     * 任务结果
     */
    private Map<String, Object> result;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    // ========== 关联信息 ==========

    /**
     * 关联章节ID列表
     */
    private List<Long> chapterIds;

    /**
     * 总章节数
     */
    private Integer totalChapters;

    /**
     * 已完成章节数
     */
    private Integer completedChapters;
}
