package com.dpbug.server.model.dto.novel;

import com.dpbug.common.domain.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目查询请求DTO
 *
 * @author dpbug
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectQueryRequest extends PageRequest {

    /**
     * 书名模糊搜索
     */
    private String title;

    /**
     * 类型筛选
     */
    private String genre;

    /**
     * 状态筛选（planning/writing/completed）
     */
    private String status;

    /**
     * 向导状态筛选（not_started/in_progress/completed）
     */
    private String wizardStatus;
}