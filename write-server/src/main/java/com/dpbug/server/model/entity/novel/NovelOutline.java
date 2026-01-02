package com.dpbug.server.model.entity.novel;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.dpbug.server.model.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 小说大纲
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_outline")
public class NovelOutline extends BaseEntity {

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 排序序号
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
     * 结构化数据 (情节点、冲突、转折等)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> structure;
}
