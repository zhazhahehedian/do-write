package com.dpbug.server.service.novel;

import com.dpbug.server.model.entity.novel.NovelCharacter;
import com.dpbug.server.model.vo.novel.CharacterStatisticsVO;
import com.dpbug.server.model.vo.novel.CharacterVO;

import java.util.List;

/**
 * 角色服务接口
 *
 * @author dpbug
 */
public interface CharacterService {

    /**
     * 批量创建角色
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @param characters 角色列表
     */
    void createBatch(Long userId, Long projectId, List<NovelCharacter> characters);

    /**
     * 获取项目所有角色
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @return 角色列表
     */
    List<CharacterVO> listByProject(Long userId, Long projectId);

    /**
     * 获取项目角色（按类型筛选）
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @param roleType  角色类型（protagonist/supporting/antagonist）
     * @return 角色列表
     */
    List<CharacterVO> listByProjectAndType(Long userId, Long projectId, String roleType);

    /**
     * 获取项目所有组织
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     * @return 组织列表
     */
    List<CharacterVO> listOrganizationsByProject(Long userId, Long projectId);

    /**
     * 获取角色详情
     *
     * @param userId      用户ID
     * @param characterId 角色ID
     * @return 角色详情
     */
    CharacterVO getById(Long userId, Long characterId);

    /**
     * 更新角色
     *
     * @param userId    用户ID
     * @param character 角色实体
     */
    void update(Long userId, NovelCharacter character);

    /**
     * 删除角色
     *
     * @param userId      用户ID
     * @param characterId 角色ID
     */
    void delete(Long userId, Long characterId);

    /**
     * 删除项目所有角色
     *
     * @param userId    用户ID
     * @param projectId 项目ID
     */
    void deleteByProject(Long userId, Long projectId);

    /**
     * 获取项目角色统计
     *
     * @param projectId 项目ID
     * @return 角色统计信息
     */
    CharacterStatisticsVO getStatistics(Long projectId);
}
