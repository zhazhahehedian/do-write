package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author dpbug
 * @description 记忆列表请求
 */
@Data
public class StoryMemoryQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID（必填）
     */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /**
     * 章节ID（可选，用于筛选指定章节）
     */
    private Long chapterId;

    /**
     * 记忆类型（可选）: plot_point/hook/foreshadow/character_event/location_event
     */
    private String memoryType;

    /**
     * 伏笔状态（可选）: 0=全部, 1=已埋下, 2=已回收
     */
    private Integer foreshadowStatus;

    /**
     * 最低重要性分数（可选）
     */
    private BigDecimal minImportance;

    /**
     * 时间线起始章节（可选）
     */
    private Integer startTimeline;

    /**
     * 时间线结束章节（可选）
     */
    private Integer endTimeline;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 20;
}
