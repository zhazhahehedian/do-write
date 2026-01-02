package com.dpbug.server.model.vo.novel;

import lombok.Data;

import java.io.Serializable;

/**
 * @author dpbug
 * @description 角色统计返回
 */
@Data
public class CharacterStatisticsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long total;
    private Long protagonistCount;
    private Long supportingCount;
    private Long antagonistCount;
    private Long organizationCount;
}
