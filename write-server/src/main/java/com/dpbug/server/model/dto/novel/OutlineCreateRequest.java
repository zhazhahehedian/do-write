package com.dpbug.server.model.dto.novel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 大纲创建请求
 *
 * @author dpbug
 */
@Data
public class OutlineCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    /**
     * 排序序号
     */
    @NotNull(message = "序号不能为空")
    private Integer orderIndex;

    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    private String title;

    /**
     * 内容摘要
     */
    private String content;

    /**
     * 结构化数据
     */
    private Map<String, Object> structure;
}

