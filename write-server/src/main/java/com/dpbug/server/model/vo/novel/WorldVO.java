package com.dpbug.server.model.vo.novel;

import java.io.Serializable;

/**
 * 世界观响应
 */
public class WorldVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 时间背景
     */
    private String worldTimePeriod;
    /**
     * 地理位置
     */
    private String worldLocation;
    /**
     * 氛围基调
     */
    private String worldAtmosphere;
    /**
     * 世界规则
     */
    private String worldRules;
}
