package com.dpbug.server.model.entity.novel;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.dpbug.server.model.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 剧情分析表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "novel_plot_analysis", autoResultMap = true)
public class NovelPlotAnalysis extends BaseEntity {

    private Long projectId;
    private Long chapterId;

    private String plotStage;

    private Integer conflictLevel;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> conflictTypes;

    private String emotionalTone;

    private BigDecimal emotionalIntensity;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object emotionalCurve;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> hooks;

    private Integer hooksCount;

    private BigDecimal hooksAvgStrength;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object foreshadows;

    private Integer foreshadowsPlanted;
    private Integer foreshadowsResolved;

    private BigDecimal overallQualityScore;
    private BigDecimal pacingScore;
    private BigDecimal engagementScore;
    private BigDecimal coherenceScore;

    private String analysisReport;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> suggestions;

    private String aiModel;
}

