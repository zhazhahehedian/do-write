package com.dpbug.server.service.novel;

import com.dpbug.common.domain.PageResult;
import com.dpbug.server.model.dto.novel.ProjectCreateRequest;
import com.dpbug.server.model.dto.novel.ProjectQueryRequest;
import com.dpbug.server.model.dto.novel.ProjectUpdateRequest;
import com.dpbug.server.model.entity.novel.NovelProject;
import com.dpbug.server.model.vo.novel.ProjectListVO;
import com.dpbug.server.model.vo.novel.ProjectStatisticsVO;
import com.dpbug.server.model.vo.novel.ProjectVO;

/**
 * 项目服务接口
 *
 * @author dpbug
 */
public interface ProjectService {

    /**
     * 创建项目
     *
     * @param userId 用户ID
     * @param dto    创建请求
     * @return 项目ID
     */
    Long create(Long userId, ProjectCreateRequest dto);

    /**
     * 获取项目详情
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @return 项目详情
     */
    ProjectVO getById(Long userId, Long projectId);

    /**
     * 更新项目
     *
     * @param userId 用户ID
     * @param dto    更新请求
     */
    void update(Long userId, ProjectUpdateRequest dto);

    /**
     * 删除项目
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     */
    void delete(Long userId, Long projectId);

    /**
     * 分页查询项目列表
     *
     * @param userId 用户ID
     * @param query  查询条件
     * @return 分页结果
     */
    PageResult<ProjectListVO> list(Long userId, ProjectQueryRequest query);

    /**
     * 获取项目统计信息
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @return 统计信息
     */
    ProjectStatisticsVO getStatistics(Long userId, Long projectId);

    /**
     * 更新项目统计（字数、角色数等）
     *
     * @param projectId 项目ID
     */
    void refreshStatistics(Long projectId);

    /**
     * 更新向导状态
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @param status    向导状态
     * @param step      向导步骤
     */
    void updateWizardStatus(Long userId, Long projectId, String status, Integer step);

    /**
     * 检查项目归属（用于其他模块调用）
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @return 项目实体
     */
    NovelProject checkOwnership(Long userId, Long projectId);

    /**
     * 更新项目状态
     * <p>
     * 当项目从规划阶段进入创作阶段时调用
     *
     * @param projectId 项目ID
     * @param status    项目状态 {@link com.dpbug.common.constant.NovelConstants.ProjectStatus}
     */
    void updateProjectStatus(Long projectId, String status);
}