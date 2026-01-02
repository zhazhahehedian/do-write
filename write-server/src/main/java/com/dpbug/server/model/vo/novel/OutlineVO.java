package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 大纲响应
 * @author dpbug
 */
@Data
public class OutlineVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 大纲id
     */
    private Long id;

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 大纲排序
     */
    private Integer orderIndex;

    /**
     * 大纲标题
     */
    private String title;

    /**
     * 大纲内容
     */
    private String content;

    /**
     * 结构化内容
     */
    private Map<String, Object> structure;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
