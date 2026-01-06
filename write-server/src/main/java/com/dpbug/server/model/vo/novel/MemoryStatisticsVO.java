package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author dpbug
 * @description 记忆统计
 */
@Data
public class MemoryStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 总记忆数
     */
    private Integer totalCount;

    /**
     * 各类型数量
     */
    private Map<String, Integer> typeCount;

    /**
     * 待回收伏笔数
     */
    private Integer pendingForeshadowCount;

    /**
     * 已回收伏笔数
     */
    private Integer resolvedForeshadowCount;

    /**
     * 覆盖章节数
     */
    private Integer coveredChapterCount;
}
