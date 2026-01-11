package com.dpbug.server.model.entity.novel;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 故事记忆实体类
 * <p>
 * 用于存储章节中提取的关键情节点、伏笔、悬念等记忆信息，
 * 支持RAG（检索增强生成）功能。
 * 不继承 BaseEntity，跟随项目生命周期，删除时物理删除。
 *
 * @author dpbug
 */
@Data
@TableName(value = "novel_story_memory", autoResultMap = true)
public class NovelStoryMemory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花算法生成）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
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
     * 完整上下文
     */
    private String fullContext;

    /**
     * 相关角色ID列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> relatedCharacters;

    /**
     * 相关地点
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> relatedLocations;

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
     * 伏笔回收时的章节ID
     */
    private Long foreshadowResolvedAt;

    /**
     * 向量数据库中的ID
     */
    private String vectorId;

    /**
     * 使用的嵌入模型
     */
    private String embeddingModel;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
