package com.dpbug.server.mapper.novel;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dpbug.server.model.entity.novel.NovelProject;
import com.dpbug.server.model.vo.novel.ProjectStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 项目Mapper接口
 *
 * @author dpbug
 */
@Mapper
public interface ProjectMapper extends BaseMapper<NovelProject> {

    /**
     * 查询项目统计信息
     *
     * @param projectId 项目ID
     * @param userId    用户ID（安全校验）
     * @return 统计信息
     */
    ProjectStatisticsVO selectStatistics(@Param("projectId") Long projectId, @Param("userId") Long userId);

    /**
     * 更新项目统计信息（字数、角色数等）
     *
     * @param projectId 项目ID
     */
    void updateStatistics(@Param("projectId") Long projectId);
}