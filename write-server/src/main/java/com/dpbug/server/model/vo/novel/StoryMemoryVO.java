package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 故事记忆响应
 *
 * @author dpbug
 */
@Data
public class StoryMemoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记忆ID
     */
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 来源章节ID
     */
    private Long chapterId;

    /**
     * 来源章节序号
     */
    private Integer chapterNumber;

    /**
     * 记忆类型: plot_point/hook/foreshadow/character_event/location_event
     */
    private String memoryType;

    /**
     * 记忆标题
     */
    private String title;

    /**
     * 记忆简化内容
     */
    private String content;

    /**
     * 重要性分数 0.00-1.00
     */
    private BigDecimal importanceScore;

    /**
     * 故事时间线（章节序号）
     */
    private Integer storyTimeline;

    /**
     * 伏笔状态: 0=普通, 1=已埋下, 2=已回收
     */
    private Integer isForeshadow;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    // ========== 关联信息 ==========

    /**
     * 相关角色名称列表
     */
    private List<String> relatedCharacterNames;

    /**
     * 相关地点
     */
    private List<String> relatedLocations;
}
