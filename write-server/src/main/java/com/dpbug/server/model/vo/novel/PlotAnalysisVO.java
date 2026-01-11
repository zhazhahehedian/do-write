package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 剧情分析返回
 */
@Data
public class PlotAnalysisVO {

    private Long id;
    private Long projectId;
    private Long chapterId;

    private String plotStage;
    private Integer conflictLevel;
    private List<String> conflictTypes;

    private String emotionalTone;
    private BigDecimal emotionalIntensity;
    private Object emotionalCurve;

    private List<Map<String, Object>> hooks;
    private Integer hooksCount;
    private BigDecimal hooksAvgStrength;

    private Object foreshadows;
    private Integer foreshadowsPlanted;
    private Integer foreshadowsResolved;

    private BigDecimal overallQualityScore;
    private BigDecimal pacingScore;
    private BigDecimal engagementScore;
    private BigDecimal coherenceScore;

    private String analysisReport;
    private List<String> suggestions;

    private String aiModel;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

