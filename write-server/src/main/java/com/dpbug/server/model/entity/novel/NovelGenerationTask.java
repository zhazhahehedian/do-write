package com.dpbug.server.model.entity.novel;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.dpbug.server.model.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 生成任务实体类
 * <p>
 * 用于跟踪批量章节生成、剧情分析等后台任务的执行状态
 *
 * @author dpbug
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "novel_generation_task", autoResultMap = true)
public class NovelGenerationTask extends BaseEntity {

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 任务类型: single_chapter/batch_chapter/analysis
     */
    private String taskType;

    /**
     * 状态: pending/running/completed/failed/cancelled
     */
    private String status;

    /**
     * 任务参数
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> params;

    /**
     * 相关章节ID列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> chapterIds;

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
    @TableField(typeHandler = JacksonTypeHandler.class)
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
}
